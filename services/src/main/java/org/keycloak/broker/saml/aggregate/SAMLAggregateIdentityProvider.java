package org.keycloak.broker.saml.aggregate;

import java.net.URI;
import java.util.Iterator;
import java.util.Optional;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.broker.provider.AuthenticationRequest;
import org.keycloak.broker.provider.IdentityBrokerException;
import org.keycloak.broker.provider.IdentityProviderDataMarshaller;
import org.keycloak.broker.saml.SAMLDataMarshaller;
import org.keycloak.broker.saml.SAMLEndpoint;
import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateMetadataStoreProvider;
import org.keycloak.broker.saml.aggregate.metadata.SAMLIdpDescriptor;
import org.keycloak.dom.saml.v2.assertion.NameIDType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.LogoutRequestType;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.protocol.saml.JaxrsSAML2BindingBuilder;
import org.keycloak.protocol.saml.SamlSessionUtils;
import org.keycloak.protocol.saml.preprocessor.SamlAuthenticationPreprocessor;
import org.keycloak.saml.SAML2AuthnRequestBuilder;
import org.keycloak.saml.SAML2LogoutRequestBuilder;
import org.keycloak.saml.SAML2NameIDPolicyBuilder;
import org.keycloak.saml.SamlProtocolExtensionsAwareBuilder.NodeGenerator;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;

public class SAMLAggregateIdentityProvider extends AbstractIdentityProvider<SAMLAggregateIdentityProviderConfig> {

    private static final String SAML_AGGREGATE_CURRENT_IDP = "saml-aggregate-current-idp";

    protected static final Logger logger =
        Logger.getLogger(SAMLAggregateIdentityProviderConfig.class);

    public SAMLAggregateIdentityProvider(KeycloakSession session,
        SAMLAggregateIdentityProviderConfig config) {
      super(session, config);
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

        URI wayfURI = uriInfo.getBaseUriBuilder().path(realmPath).queryParam("provider", providerAlias)
                .build();

        return Response.temporaryRedirect(wayfURI).build();
    }


    @Override
    public Response performLogin(AuthenticationRequest request) {

        try {

            UriInfo uriInfo = request.getUriInfo();
            RealmModel realm = request.getRealm();


            String providerAlias = getConfig().getAlias();

            if (!uriInfo.getQueryParameters().containsKey("entity_id")) {
                return redirectToWayf(request);
            }

            String entityId = uriInfo.getQueryParameters().get("entity_id").get(0);


            SAMLIdpDescriptor idp = getIdentityProviderFromEntityId(realm, providerAlias, entityId);

            String issuerURL = getEntityId(uriInfo, realm);

            String protocolBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get();

            if (idp.isPostBindingResponse()) {
                protocolBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get();
            }

            String nameIDPolicyFormat = JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get();
            Boolean isForceAuthn = false;

            String assertionConsumerServiceUrl = request.getRedirectUri();

            String destinationUrl = idp.getSingleSignOnServiceUrl();

            SAML2AuthnRequestBuilder authnRequestBuilder = new SAML2AuthnRequestBuilder()
                .assertionConsumerUrl(assertionConsumerServiceUrl)
                .destination(destinationUrl)
                .issuer(issuerURL)
                .forceAuthn(isForceAuthn)
                .protocolBinding(protocolBinding)
                .nameIdPolicy(SAML2NameIDPolicyBuilder.format(nameIDPolicyFormat));
            JaxrsSAML2BindingBuilder binding =
                new JaxrsSAML2BindingBuilder(session).relayState(request.getState().getEncoded());

            boolean postBinding = idp.isPostBindingResponse();

            AuthnRequestType authnRequest = authnRequestBuilder.createAuthnRequest();
            for (Iterator<SamlAuthenticationPreprocessor> it =
                SamlSessionUtils.getSamlAuthenticationPreprocessorIterator(session); it.hasNext();) {
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

      session.setAttribute(SAML_AGGREGATE_CURRENT_IDP, result.get());
      return result.get();
    }

    @Override
    public Object callback(RealmModel realm, AuthenticationCallback callback, EventBuilder event) {
      // cosa mi arriva qui?
      SAMLIdpDescriptor idp = session.getAttribute(SAML_AGGREGATE_CURRENT_IDP, SAMLIdpDescriptor.class);
      return null; //new SAMLEndpoint(realm, this, getConfig(), callback, destinationValidator);
    }

    @Override
    public IdentityProviderDataMarshaller getMarshaller() {
        return new SAMLDataMarshaller();
    }

    private String getEntityId(UriInfo uriInfo, RealmModel realm) {
        return UriBuilder.fromUri(uriInfo.getBaseUri())
            .path("realms")
            .path(realm.getName())
            .build()
            .toString();
    }

    @Override
    public Response keycloakInitiatedBrowserLogout(KeycloakSession session, UserSessionModel userSession, UriInfo uriInfo, RealmModel realm) {

        SAMLIdpDescriptor idp = (SAMLIdpDescriptor) session.getAttribute(SAML_AGGREGATE_CURRENT_IDP);
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
                .sessionIndex(userSession.getNote(SAMLEndpoint.SAML_FEDERATED_SESSION_INDEX))
                .nameId(NameIDType.deserializeFromString(userSession.getNote(SAMLEndpoint.SAML_FEDERATED_SUBJECT_NAMEID)))
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

}
