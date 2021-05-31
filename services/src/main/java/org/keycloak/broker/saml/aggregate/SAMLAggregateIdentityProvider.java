package org.keycloak.broker.saml.aggregate;

import java.net.URI;
import java.security.KeyPair;
import java.util.Iterator;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.*;
import org.keycloak.broker.saml.SAMLDataMarshaller;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateMetadataStoreProvider;
import org.keycloak.broker.saml.aggregate.metadata.SAMLIdpDescriptor;
import org.keycloak.common.constants.GenericConstants;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.ServerCookie;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.EventBuilder;
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.*;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder.NodeGenerator;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.resources.IdentityBrokerService;
import org.keycloak.services.resources.LoginActionsService;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.MediaType;

public class SAMLAggregateIdentityProvider extends AbstractIdentityProvider<SAMLAggregateIdentityProviderConfig> {

    public static final String SAML_AGGREGATE_COOKIE_NAME = "SAML_AGGREGATE_ENTITY_ID";

    protected static final Logger logger =
        Logger.getLogger(SAMLAggregateIdentityProviderConfig.class);

    private final DestinationValidator destinationValidator;

    public SAMLAggregateIdentityProvider(KeycloakSession session,
        SAMLAggregateIdentityProviderConfig config, DestinationValidator destinationValidator) {
      super(session, config);
      this.destinationValidator = destinationValidator;
    }

    @Override
    public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
      return Response.ok(identity.getToken()).build();
    }


    protected Response redirectToWayf(AuthenticationRequest request) {

        final String providerAlias = getConfig().getAlias();
        final String realmName = request.getRealm().getName();

        KeycloakUriInfo uriInfo = session.getContext().getUri();

        String realmPath = String.format("realms/%s/saml-wayf-page", request.getRealm().getName());

        String sessionCode = uriInfo.getQueryParameters().get(LoginActionsService.SESSION_CODE).get(0);
        String tabId = uriInfo.getQueryParameters().get(Constants.TAB_ID).get(0);
        String clientId = uriInfo.getQueryParameters().get("client_id").get(0);
        URI wayfURI = uriInfo.getBaseUriBuilder().path(realmPath).queryParam("provider", providerAlias)
                .queryParam("tabId", tabId)
                .queryParam("clientId", clientId)
                .queryParam("sessionCode", sessionCode)
                .build();

        return Response.temporaryRedirect(wayfURI).type(MediaType.TEXT_HTML_UTF_8_TYPE).build();
    }


    @Override
    public Response performLogin(AuthenticationRequest request) {

        try {

            UriInfo uriInfo = request.getUriInfo();

            if (!uriInfo.getQueryParameters().containsKey("entity_id")) {
                return redirectToWayf(request);
            }
            String entityId = uriInfo.getQueryParameters().get("entity_id").get(0);

            RealmModel realm = request.getRealm();
            String issuerURL = getEntityId(uriInfo, realm);

            String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

            String providerAlias = getConfig().getAlias();
            SAMLIdpDescriptor idp = getIdentityProviderFromEntityId(realm, providerAlias, entityId);

            String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);
            boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());

            CookieHelper.addCookie(SAML_AGGREGATE_COOKIE_NAME, entityId, cookiePath, null, "The entityId of tht selected idp", -1, sslRequired, true, ServerCookie.SameSiteAttributeValue.NONE);
            //String assertionConsumerServiceUrl = request.getRedirectUri() + "?entity_id=" + entityId + "&provider_alias=" + providerAlias;
            String assertionConsumerServiceUrl = request.getRedirectUri();

            if (idp.isPostBindingResponse()) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }

            String destinationUrl = idp.getSingleSignOnServiceUrl();

            SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                .assertionConsumerUrl(assertionConsumerServiceUrl)
                .destination(destinationUrl)
                .issuer(issuerURL)
                .forceAuthn(getConfig().isForceAuthn())
                .protocolBinding(protocolBinding)
                .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat));
            JaxrsSAML2BindingBuilder binding =
                new JaxrsSAML2BindingBuilder(session).relayState(request.getState().getEncoded());

            boolean postBinding = idp.isPostBindingResponse();

            if (getConfig().isWantAuthnRequestsSigned()) {
                KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);

                KeyPair keypair = new KeyPair(keys.getPublicKey(), keys.getPrivateKey());

                String keyName = getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
                binding.signWith(keyName, keypair);
                binding.signatureAlgorithm(getSignatureAlgorithm());
                binding.signDocument();
                if (! postBinding && getConfig().isAddExtensionsElementWithKeyInfo()) {    // Only include extension if REDIRECT binding and signing whole SAML protocol message
                    authnRequestBuilder.addExtension(new KeycloakKeySamlExtensionGenerator(keyName));
                }
            }

            AuthnRequestType authnRequest = authnRequestBuilder.createAuthnRequest();
            for(Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext(); ) {
                authnRequest = it.next().beforeSendingLoginRequest(authnRequest, request.getAuthenticationSession());
            }

            if (postBinding) {
                return binding.postBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
            } else {
                return binding.redirectBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
            }
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not create authentication request.", e);
        }
    }

    private SAMLIdpDescriptor getIdentityProviderFromEntityId(RealmModel realm, String providerAlias, String entityId) {

      SAMLAggregateMetadataStoreProvider md =
          session.getProvider(SAMLAggregateMetadataStoreProvider.class);

      Optional<SAMLIdpDescriptor> result = md.lookupIdpByEntityId(realm, providerAlias, entityId);

      if (!result.isPresent()) {
        throw new IdentityBrokerException(
            "Could not create authentication request. entity_id " + entityId + " not found.");
      }

      return result.get();
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
      String entityId = session.getContext().getRequestHeaders().getCookies().get(SAML_AGGREGATE_COOKIE_NAME + CookieHelper.LEGACY_COOKIE).getValue();
      SAMLIdpDescriptor idp = getIdentityProviderFromEntityId(realm, getConfig().getAlias(), entityId);
      return new SAMLAggregateEndpoint(realm, this, getConfig(), callback, destinationValidator, idp);
    }

    @Override
    public IdentityProviderDataMarshaller getMarshaller() {
        return new SAMLDataMarshaller();
    }

    private String getEntityId(UriInfo uriInfo, RealmModel realm) {
        return UriBuilder.fromUri(uriInfo.getBaseUri())
            .path("realms")
            .path(realm.getName())
            .path(getConfig().getAlias())
            .build()
            .toString();
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {

        String entityId = session.getContext().getRequestHeaders().getCookies().get(SAML_AGGREGATE_COOKIE_NAME + CookieHelper.LEGACY_COOKIE).getValue();
        SAMLIdpDescriptor idp = getIdentityProviderFromEntityId(realm, getConfig().getAlias(), entityId);

        String singleLogoutServiceUrl = idp.getSingleLogoutServiceUrl();
        if (singleLogoutServiceUrl == null || singleLogoutServiceUrl.trim().equals("")) return null;

        try {
            LogoutRequestType logoutRequest = buildLogoutRequest(userSession, uriInfo, realm, singleLogoutServiceUrl);
            JaxrsSAML2BindingBuilder binding = buildLogoutBinding(session, userSession, realm);
            if (idp.isPostBindingLogout()) {
                return binding.postBinding(SAML2Request.convert(logoutRequest)).request(singleLogoutServiceUrl);
            } else {
                return binding.redirectBinding(SAML2Request.convert(logoutRequest)).request(singleLogoutServiceUrl);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected LogoutRequestType buildLogoutRequest(UserSessionModel userSession, UriInfo uriInfo, RealmModel realm, String singleLogoutServiceUrl, NodeGenerator... extensions) throws ConfigurationException {
        SAML2LogoutRequestBuilder logoutBuilder = new SAML2LogoutRequestBuilder()
                .assertionExpiration(realm.getAccessCodeLifespan())
                .issuer(getEntityId(uriInfo, realm))
                .sessionIndex(userSession.getNote(SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_INDEX))
                .nameId(NameIDType.deserializeFromString(userSession.getNote(SAMLAggregateEndpoint.SAML_FEDERATED_SUBJECT_NAMEID)))
                .destination(singleLogoutServiceUrl);
        LogoutRequestType logoutRequest = logoutBuilder.createLogoutRequest();
        for (NodeGenerator extension : extensions) {
            logoutBuilder.addExtension(extension);
        }
        for (Iterator<SamlAuthenticationPreprocessor> it = SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
            logoutRequest = it.next().beforeSendingLogoutRequest(logoutRequest, userSession, null);
        }
        return logoutRequest;
    }

    private JaxrsSAML2BindingBuilder buildLogoutBinding(KeycloakSession session, UserSessionModel userSession, RealmModel realm) {
        JaxrsSAML2BindingBuilder binding = new JaxrsSAML2BindingBuilder(session)
                .relayState(userSession.getId());
        return binding;
    }

    public SignatureAlgorithm getSignatureAlgorithm() {
        String alg = getConfig().getSignatureAlgorithm();
        if (alg != null) {
            SignatureAlgorithm algorithm = SignatureAlgorithm.valueOf(alg);
            if (algorithm != null) return algorithm;
        }
        return SignatureAlgorithm.RSA_SHA256;
    }

    @Override
    public Response export(UriInfo uriInfo, RealmModel realm, String format) {

        String authnBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

        if (getConfig().isPostBindingAuthnRequest()) {
            authnBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
        }

        String endpoint = uriInfo.getBaseUriBuilder()
                .path("realms").path(realm.getName())
                .path("broker")
                .path(getConfig().getProviderId())
                .path("endpoint*")
                .build().toString();

        boolean wantAuthnRequestsSigned = getConfig().isWantAuthnRequestsSigned();
        boolean wantAssertionsSigned = getConfig().isWantAssertionsSigned();
        boolean wantAssertionsEncrypted = getConfig().isWantAssertionsEncrypted();
        String entityId = getEntityId(uriInfo, realm);
        String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

        StringBuilder signingKeysString = new StringBuilder();
        StringBuilder encryptionKeysString = new StringBuilder();
        Set<RsaKeyMetadata> keys = new TreeSet<>((o1, o2) -> o1.getStatus() == o2.getStatus() // Status can be only PASSIVE OR ACTIVE, push PASSIVE to end of list
                ? (int) (o2.getProviderPriority() - o1.getProviderPriority())
                : (o1.getStatus() == KeyStatus.PASSIVE ? 1 : -1));
        keys.addAll(session.keys().getRsaKeys(realm));
        for (RsaKeyMetadata key : keys) {
            addKeyInfo(signingKeysString, key, KeyTypes.SIGNING.value());

            if (key.getStatus() == KeyStatus.ACTIVE) {
                addKeyInfo(encryptionKeysString, key, KeyTypes.ENCRYPTION.value());
            }
        }
        String descriptor = SPMetadataDescriptor.getSPDescriptor(authnBinding, endpoint, endpoint,
                wantAuthnRequestsSigned, wantAssertionsSigned, wantAssertionsEncrypted,
                entityId, nameIDPolicyFormat, signingKeysString.toString(), encryptionKeysString.toString());

        return Response.ok(descriptor, javax.ws.rs.core.MediaType.APPLICATION_XML_TYPE).build();
    }

    private static void addKeyInfo(StringBuilder target, RsaKeyMetadata key, String purpose) {
        if (key == null) {
            return;
        }

        target.append(SPMetadataDescriptor.xmlKeyInfo("        ", key.getKid(), PemUtils.encodeCertificate(key.getCertificate()), purpose, true));
    }

    @Override
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context)  {
        ResponseType responseType = (ResponseType)context.getContextData().get(SAMLAggregateEndpoint.SAML_LOGIN_RESPONSE);
        AssertionType assertion = (AssertionType)context.getContextData().get(SAMLAggregateEndpoint.SAML_ASSERTION);
        SubjectType subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        NameIDType subjectNameID = (NameIDType) subType.getBaseID();
        authSession.setUserSessionNote(SAMLAggregateEndpoint.SAML_FEDERATED_SUBJECT_NAMEID, subjectNameID.serializeAsString());
        AuthnStatementType authn =  (AuthnStatementType)context.getContextData().get(SAMLAggregateEndpoint.SAML_AUTHN_STATEMENT);
        if (authn != null && authn.getSessionIndex() != null) {
            authSession.setUserSessionNote(SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_INDEX, authn.getSessionIndex());
            authSession.setUserSessionNote(SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_ENTITY_ID, responseType.getIssuer().getValue());
            authSession.setUserSessionNote(SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_PROVIDER_ALIAS, SAMLAggregateIdentityProviderFactory.PROVIDER_ID);
        }
    }
}
