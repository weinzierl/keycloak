package org.keycloak.testsuite.oidc;

import static org.junit.Assert.assertEquals;

import java.net.URI;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.federation.common.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.common.beans.OPMetadata;
import org.keycloak.protocol.oidc.federation.common.exceptions.BadSigningOrEncryptionException;
import org.keycloak.protocol.oidc.federation.common.exceptions.UnparsableException;
import org.keycloak.protocol.oidc.federation.common.processes.TrustChainProcessor;
import org.keycloak.protocol.oidc.federation.op.rest.OIDCFederationResourceProvider;
import org.keycloak.protocol.oidc.federation.op.spi.OIDCFedOPWellKnownProvider;
import org.keycloak.protocol.oidc.federation.op.spi.OIDCFedOPWellKnownProviderFactory;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationProviderFactory;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.util.OAuthClient;

import com.fasterxml.jackson.core.JsonProcessingException;

public class OIDCFederationTest extends AbstractKeycloakTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"),
            RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Test
    public void testWellKnown() throws UnparsableException, BadSigningOrEncryptionException, JsonProcessingException {
        Client client = ClientBuilder.newClient();
        try {

            TrustChainProcessor processor= new TrustChainProcessor();
            EntityStatement statement = processor.parseAndValidateSelfSigned(getOIDCDiscoveryConfiguration(client));

            // check statement fields
            assertEquals(statement.getIssuer(),
                RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            assertEquals(statement.getSubject(),
                RealmsResource.realmBaseUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            Assert.assertNotNull(statement.getAuthorityHints());
            assertEquals("AuthorityHints.size", 2, statement.getAuthorityHints().size());
            assertContains(statement.getAuthorityHints(), "http://localhost:8080/intermediate1",
                "http://localhost:8080/intermediate2");
            Assert.assertNotNull(statement.getMetadata());
            Assert.assertNotNull(statement.getMetadata().getOp());
            // check federation open id provider configuration
            OPMetadata op = statement.getMetadata().getOp();
            assertEquals(op.getFederationRegistrationEndpoint(), OIDCFederationResourceProvider
                .federationExplicitRegistration(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            Assert.assertNotNull(op.getClientRegistrationTypesSupported());
            assertEquals("ClientRegistrationTypesSupporte.size", 2, op.getClientRegistrationTypesSupported().size());
            assertContains(op.getClientRegistrationTypesSupported(),
                OIDCFedOPWellKnownProvider.CLIENT_REGISTRATION_TYPES_SUPPORTED.get(0),
                OIDCFedOPWellKnownProvider.CLIENT_REGISTRATION_TYPES_SUPPORTED.get(1));
            // client_registration_authn_methods_supported and pushed_authorization_request_endpoint may be added in future
            // same as oidc configuration
            assertEquals(op.getAuthorizationEndpoint(),
                OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            assertEquals(op.getTokenEndpoint(), oauth.getAccessTokenUrl());
            assertEquals(op.getUserinfoEndpoint(), OIDCLoginProtocolService
                .userInfoUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            assertEquals(op.getJwksUri(), oauth.getCertsUrl("test"));

            String registrationUri = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT).path(RealmsResource.class)
                .path(RealmsResource.class, "getClientsService").path(ClientRegistrationService.class, "provider")
                .build("test", OIDCClientRegistrationProviderFactory.ID).toString();
            assertEquals(op.getRegistrationEndpoint(), registrationUri);

            // Support standard + implicit + hybrid flow
            assertContains(op.getResponseTypesSupported(), OAuth2Constants.CODE, OIDCResponseType.ID_TOKEN, "id_token token",
                "code id_token", "code token", "code id_token token");
            assertContains(op.getGrantTypesSupported(), OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.IMPLICIT);
            assertContains(op.getResponseModesSupported(), "query", "fragment");

            Assert.assertNames(op.getSubjectTypesSupported(), "pairwise", "public");

            // Signature algorithms
            Assert.assertNames(op.getIdTokenSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512,
                Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512,
                Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);
            Assert.assertNames(op.getUserInfoSigningAlgValuesSupported(), "none", Algorithm.PS256, Algorithm.PS384,
                Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384,
                Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);
            Assert.assertNames(op.getRequestObjectSigningAlgValuesSupported(), "none", Algorithm.PS256, Algorithm.PS384,
                Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384,
                Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);

            // Encryption algorithms
            Assert.assertNames(op.getIdTokenEncryptionAlgValuesSupported(), JWEConstants.RSA1_5, JWEConstants.RSA_OAEP);
            Assert.assertNames(op.getIdTokenEncryptionEncValuesSupported(), JWEConstants.A128CBC_HS256, JWEConstants.A128GCM,
                JWEConstants.A192CBC_HS384, JWEConstants.A192GCM, JWEConstants.A256CBC_HS512, JWEConstants.A256GCM);

            // Client authentication
            Assert.assertNames(op.getTokenEndpointAuthMethodsSupported(), "client_secret_basic", "client_secret_post",
                "private_key_jwt", "client_secret_jwt", "tls_client_auth");
            Assert.assertNames(op.getTokenEndpointAuthSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384,
                Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384,
                Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);

            // Claims
            assertContains(op.getClaimsSupported(), IDToken.NAME, IDToken.EMAIL, IDToken.PREFERRED_USERNAME,
                IDToken.FAMILY_NAME, IDToken.ACR);
            Assert.assertNames(op.getClaimTypesSupported(), "normal");
            Assert.assertFalse(op.getClaimsParameterSupported());

            // Scopes supported
            Assert.assertNames(op.getScopesSupported(), OAuth2Constants.SCOPE_OPENID, OAuth2Constants.OFFLINE_ACCESS,
                OAuth2Constants.SCOPE_PROFILE, OAuth2Constants.SCOPE_EMAIL, OAuth2Constants.SCOPE_PHONE,
                OAuth2Constants.SCOPE_ADDRESS, OIDCLoginProtocolFactory.ROLES_SCOPE, OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE,
                OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE);

            // Request and Request_Uri
            Assert.assertTrue(op.getRequestParameterSupported());
            Assert.assertTrue(op.getRequestUriParameterSupported());

            // KEYCLOAK-7451 OAuth Authorization Server Metadata for Proof Key for Code Exchange
            // PKCE support
            Assert.assertNames(op.getCodeChallengeMethodsSupported(), OAuth2Constants.PKCE_METHOD_PLAIN,
                OAuth2Constants.PKCE_METHOD_S256);

            // KEYCLOAK-6771 Certificate Bound Token
            // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.2
            Assert.assertTrue(op.getTlsClientCertificateBoundAccessTokens());

        } finally {
            client.close();
        }
    }





    private String getOIDCDiscoveryConfiguration(Client client) {
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT))
            .build("test", OIDCFedOPWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        Response response = oidcDiscoveryTarget.request().get();

        return response.readEntity(String.class);
    }


    private void assertContains(List<String> actual, String... expected) {
        for (String exp : expected) {
            Assert.assertTrue(actual.contains(exp));
        }
    }

}
