package org.keycloak.broker.saml.aggregate;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.*;

import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.crypto.dsig.CanonicalizationMethod;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamWriter;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.*;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.broker.saml.SAMLDataMarshaller;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.SAMLIdentityProvider;
import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateMetadataStoreProvider;
import org.keycloak.broker.saml.aggregate.metadata.SAMLIdpDescriptor;
import org.keycloak.common.util.PemUtils;
import org.keycloak.common.util.ServerCookie;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.assertion.AssertionType;
import org.keycloak.dom.saml.v2.assertion.AuthnStatementType;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.assertion.SubjectType;
import org.keycloak.dom.saml.v2.metadata.AttributeConsumingServiceType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.events.Details;
import org.keycloak.events.EventBuilder;
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.mappers.SamlMetadataDescriptorUpdater;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.*;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder.NodeGenerator;
import org.keycloak.saml.common.constants.GeneralConstants;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.common.util.StaxUtil;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.api.saml.v2.sig.SAML2Signature;
import org.keycloak.saml.processing.core.saml.v2.writers.SAMLMetadataWriter;
import org.keycloak.saml.processing.core.util.KeycloakKeySamlExtensionGenerator;
import org.keycloak.saml.validators.DestinationValidator;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.util.CookieHelper;
import org.keycloak.sessions.AuthenticationSessionModel;
import org.keycloak.utils.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class SAMLAggregateIdentityProvider extends AbstractIdentityProvider<SAMLAggregateIdentityProviderConfig> {

    private static final String SAML_AGGREGATE_WAYF_LOGIN_COOKIE_NAME_PREFIX = "SAML_AGGREGATE_WAYF_LOGIN_";
    private static final String SAML_AGGREGATE_WAYF_LINKING_COOKIE_NAME_PREFIX = "SAML_AGGREGATE_WAYF_LINKING_";

    private static final String SAML_AGGREGATE_LOGIN_COOKIE_NAME_PREFIX = "SAML_AGGREGATE_LOGIN_";
    private static final String SAML_AGGREGATE_LINKING_COOKIE_NAME_PREFIX = "SAML_AGGREGATE_LINKING_";

    private static final String SAML_AGGREGATE_COOKIE_COMMENT = "The entityId of tht selected idp";

    public enum RequestType {
        LOGIN,
        LINKING,
        LOGOUT;
    }

    //private final List<SAMLIdentityProvider> idps;

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

        KeycloakUriInfo uriInfo = session.getContext().getUri();

        String realmPath = String.format("realms/%s/saml-wayf-page", request.getRealm().getName());

        String clientId = uriInfo.getQueryParameters().get("client_id").get(0);

        URI wayfURI = uriInfo.getBaseUriBuilder().path(realmPath)
                .queryParam("provider", providerAlias)
                .queryParam("clientId", clientId)
                .build();

        return Response.temporaryRedirect(wayfURI).type(MediaType.TEXT_HTML_UTF_8_TYPE).build();
    }


    @Override
    public Response performLogin(AuthenticationRequest request) {

        RealmModel realm = request.getRealm();
        AuthenticationManager.AuthResult cookieResult = AuthenticationManager.authenticateIdentityCookie(session, realm, true);
        Optional<String> entityId;
        RequestType requestType;

        if (cookieResult == null) {
            // not logged in
            requestType = RequestType.LOGIN;
        } else {
            // linking or logout?
            // logged in => linking account
            requestType = RequestType.LINKING;
        }

        entityId = getEntityIdFromWayfCookie(requestType);
        expireWayfCookie(requestType);

        if (!entityId.isPresent()) {
            return redirectToWayf(request);
        }

        try {

            UriInfo uriInfo = request.getUriInfo();

            String issuerURL = getEntityId(uriInfo, realm);

            String nameIDPolicyFormat = getConfig().getNameIDPolicyFormat();

            if (nameIDPolicyFormat == null) {
                nameIDPolicyFormat =  JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            }

            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

            String providerAlias = getConfig().getAlias();
            SAMLIdpDescriptor idp = getIdentityProviderFromEntityId(realm, providerAlias, entityId.get());

            String assertionConsumerServiceUrl = request.getRedirectUri();

            if (idp.isPostBindingResponse()) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }

            createCookie(uriInfo, realm, requestType, entityId.get());
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

            if (authnRequest.getDestination() != null) {
                destinationUrl = authnRequest.getDestination().toString();
            }

            // Save the current RequestID in the Auth Session as we need to verify it against the ID returned from the IdP
            request.getAuthenticationSession().setClientNote(SamlProtocol.SAML_REQUEST_ID, authnRequest.getID());

            if (postBinding) {
                return binding.postBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
            } else {
                return binding.redirectBinding(authnRequestBuilder.toDocument()).request(destinationUrl);
            }
        } catch (Exception e) {
            throw new IdentityBrokerException("Could not create authentication request.", e);
        }
    }

    private void createCookie(UriInfo uriInfo, RealmModel realm, RequestType requestType, String value) {
        String cookieName;
        switch (requestType) {
            case LINKING:
                cookieName = SAML_AGGREGATE_LINKING_COOKIE_NAME_PREFIX + getConfig().getAlias();
                break;
            default:
                cookieName = SAML_AGGREGATE_LOGIN_COOKIE_NAME_PREFIX + getConfig().getAlias();
        }
        String cookiePath = AuthenticationManager.getRealmCookiePath(realm, uriInfo);
        boolean sslRequired = realm.getSslRequired().isRequired(session.getContext().getConnection());
        CookieHelper.addCookie(cookieName, value, cookiePath, null, SAML_AGGREGATE_COOKIE_COMMENT, -1, sslRequired, true, ServerCookie.SameSiteAttributeValue.NONE);
    }

    private void expireCookie(String name, String path, Integer maxAge, Boolean secure, Boolean httpOnly) {
        CookieHelper.addCookie(name, "", path, null, "Expiring cookie", maxAge, secure, httpOnly);
    }

    private void expireCallbackCookie(RequestType requestType) {
        switch (requestType) {
            case LINKING:
                expireCookie(SAML_AGGREGATE_LINKING_COOKIE_NAME_PREFIX + getConfig().getAlias(), "/", 0, false, false);
                break;
            case LOGIN:
                expireCookie(SAML_AGGREGATE_LOGIN_COOKIE_NAME_PREFIX + getConfig().getAlias(), "/", 0, false, false);
                break;
            default:
        }
    }

    private void expireWayfCookie(RequestType requestType) {
        switch (requestType) {
            case LINKING:
                expireCookie(SAML_AGGREGATE_WAYF_LINKING_COOKIE_NAME_PREFIX + getConfig().getAlias(), "/", 0, false, false);
                break;
            default:
                expireCookie(SAML_AGGREGATE_WAYF_LOGIN_COOKIE_NAME_PREFIX + getConfig().getAlias(), "/", 0, false, false);
        }

    }
    private Optional<String> getEntityIdFromWayfCookie(RequestType requestType) {
        Set<String> values;
        switch (requestType) {
            case LINKING:
                values = CookieHelper.getCookieValue(SAML_AGGREGATE_WAYF_LINKING_COOKIE_NAME_PREFIX + getConfig().getAlias());
                break;
            default:
                values = CookieHelper.getCookieValue(SAML_AGGREGATE_WAYF_LOGIN_COOKIE_NAME_PREFIX + getConfig().getAlias());
        }
        if (values.size() > 0) {
            try {
                String result = java.net.URLDecoder.decode(values.iterator().next(), StandardCharsets.UTF_8.name());
                return Optional.of(result);
            } catch (UnsupportedEncodingException e) {
                return Optional.empty();
            }
        }
        return Optional.empty();
    }

    private String getLoginCookieName() {
        return SAML_AGGREGATE_LOGIN_COOKIE_NAME_PREFIX + getConfig().getAlias() + CookieHelper.LEGACY_COOKIE;
    }

    private String getLinkingCookieName() {
        return SAML_AGGREGATE_LINKING_COOKIE_NAME_PREFIX + getConfig().getAlias() + CookieHelper.LEGACY_COOKIE;
    }

    private Optional<String> getEntityIdFromCookie(RequestType requestType) {
        String legacyName;
        switch (requestType) {
            case LINKING:
                legacyName = getLinkingCookieName();
                break;
            default:
                legacyName = getLoginCookieName();
        }
        Cookie c = session.getContext().getRequestHeaders().getCookies().get(legacyName);
        if (c != null) {
            return Optional.of(c.getValue());
        }
        return Optional.empty();
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

        AuthenticationManager.AuthResult cookieResult = AuthenticationManager.authenticateIdentityCookie(session, realm, true);
        RequestType requestType;
        if (cookieResult == null) {
            // not logged in
            requestType = RequestType.LOGIN;
        } else {
            if (cookieResult.getSession().getNotes().get(Details.IDENTITY_PROVIDER) != null) {
                requestType = RequestType.LOGOUT;
            } else {
                // logged in => linking account === OR LOGOUT!
                requestType = RequestType.LINKING;
            }
        }
        Optional<String> entityId;
        if (RequestType.LOGOUT.equals(requestType)) {
            entityId = Optional.ofNullable(cookieResult.getSession().getNote(SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_ENTITY_ID));
        } else {
            entityId = getEntityIdFromCookie(requestType);
        }
        if (!entityId.isPresent()) {
            throw new RuntimeException("Cookie not found!");
        }
        expireCallbackCookie(requestType);
        SAMLIdpDescriptor idp = getIdentityProviderFromEntityId(realm, getConfig().getAlias(), entityId.get());
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

        Map<String, String> notes = userSession.getNotes();
        String entityIdKey = SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_ENTITY_ID;
        Optional<String> entityId = Optional.ofNullable(notes.get(entityIdKey));
        if (!entityId.isPresent()) {
            throw new RuntimeException("EntityId not found in session!");
        }
        Optional<String> providerAlias = Optional.ofNullable(userSession.getNotes().get(Details.IDENTITY_PROVIDER));
        if (!providerAlias.isPresent()) {
            throw new RuntimeException("ProviderAlias not found in session!");
        }
        SAMLIdpDescriptor idp = getIdentityProviderFromEntityId(realm, providerAlias.get(), entityId.get());

        String singleLogoutServiceUrl = idp.getSingleLogoutServiceUrl();
        if (singleLogoutServiceUrl == null || singleLogoutServiceUrl.trim().equals("")) return null;

        if (getConfig().isBackchannelSupported()) {
            backchannelLogout(session, userSession, uriInfo, realm);
            return null;
        } else {
            try {
                LogoutRequestType logoutRequest = buildLogoutRequest(userSession, uriInfo, realm, singleLogoutServiceUrl);
                if (logoutRequest.getDestination() != null) {
                    singleLogoutServiceUrl = logoutRequest.getDestination().toString();
                }
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
    }

    @Override
    public void backchannelLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {
        String singleLogoutServiceUrl = getConfig().getSingleLogoutServiceUrl();
        if (singleLogoutServiceUrl == null || singleLogoutServiceUrl.trim().equals("") || !getConfig().isBackchannelSupported()) return;
        JaxrsSAML2BindingBuilder binding = buildLogoutBinding(session, userSession, realm);
        try {
            LogoutRequestType logoutRequest = buildLogoutRequest(userSession, uriInfo, realm, singleLogoutServiceUrl);
            if (logoutRequest.getDestination() != null) {
                singleLogoutServiceUrl = logoutRequest.getDestination().toString();
            }
            int status = SimpleHttp.doPost(singleLogoutServiceUrl, session)
                    .param(GeneralConstants.SAML_REQUEST_KEY, binding.postBinding(SAML2Request.convert(logoutRequest)).encoded())
                    .param(GeneralConstants.RELAY_STATE, userSession.getId()).asStatus();
            boolean success = status >=200 && status < 400;
            if (!success) {
                logger.warn("Failed saml backchannel broker logout to: " + singleLogoutServiceUrl);
            }
        } catch (Exception e) {
            logger.warn("Failed saml backchannel broker logout to: " + singleLogoutServiceUrl, e);
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
        if (getConfig().isWantAuthnRequestsSigned()) {
            KeyManager.ActiveRsaKey keys = session.keys().getActiveRsaKey(realm);
            String keyName = getConfig().getXmlSigKeyInfoKeyNameTransformer().getKeyName(keys.getKid(), keys.getCertificate());
            binding.signWith(keyName, keys.getPrivateKey(), keys.getPublicKey(), keys.getCertificate())
                    .signatureAlgorithm(getSignatureAlgorithm())
                    .signDocument();
        }
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
    public void authenticationFinished(AuthenticationSessionModel authSession, BrokeredIdentityContext context) {
        ResponseType responseType = (ResponseType) context.getContextData().get(SAMLAggregateEndpoint.SAML_LOGIN_RESPONSE);
        AssertionType assertion = (AssertionType) context.getContextData().get(SAMLAggregateEndpoint.SAML_ASSERTION);
        SubjectType subject = assertion.getSubject();
        SubjectType.STSubType subType = subject.getSubType();
        if (subType != null) {
            NameIDType subjectNameID = (NameIDType) subType.getBaseID();
            authSession.setUserSessionNote(SAMLAggregateEndpoint.SAML_FEDERATED_SUBJECT_NAMEID, subjectNameID.serializeAsString());
        }
        AuthnStatementType authn = (AuthnStatementType) context.getContextData().get(SAMLAggregateEndpoint.SAML_AUTHN_STATEMENT);
        if (authn != null && authn.getSessionIndex() != null) {
            authSession.setUserSessionNote(SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_INDEX, authn.getSessionIndex());
        }
        if (authSession.getRedirectUri().endsWith("account/identity")) {
           // linking
           authSession.setUserSessionNote(Details.IDENTITY_PROVIDER, null);
        } else {
           // login
           authSession.setUserSessionNote(Details.IDENTITY_PROVIDER, context.getIdpConfig().getAlias());
           authSession.setUserSessionNote(Details.IDENTITY_PROVIDER_USERNAME, context.getUsername());
           authSession.setUserSessionNote(SAMLAggregateEndpoint.SAML_FEDERATED_SESSION_ENTITY_ID, responseType.getIssuer().getValue());
        }
    }
}
