package org.keycloak.testsuite.oidc;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.federation.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.beans.RPMetadata;
import org.keycloak.protocol.oidc.federation.beans.RPMetadataPolicy;
import org.keycloak.protocol.oidc.federation.beans.OPMetadata;
import org.keycloak.protocol.oidc.federation.exceptions.BadSigningOrEncryptionException;
import org.keycloak.protocol.oidc.federation.exceptions.MetadataPolicyCombinationException;
import org.keycloak.protocol.oidc.federation.exceptions.MetadataPolicyException;
import org.keycloak.protocol.oidc.federation.exceptions.UnparsableException;
import org.keycloak.protocol.oidc.federation.helpers.FedUtils;
import org.keycloak.protocol.oidc.federation.helpers.MetadataPolicyUtils;
import org.keycloak.protocol.oidc.federation.op.rest.OIDCFederationResourceProvider;
import org.keycloak.protocol.oidc.federation.op.spi.OIDCFedOPWellKnownProvider;
import org.keycloak.protocol.oidc.federation.op.spi.OIDCFedOPWellKnownProviderFactory;
import org.keycloak.protocol.oidc.federation.processes.TrustChainProcessor;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationProviderFactory;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.arquillian.annotation.ModelTest;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.util.JsonSerialization;

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

    @Test
    @ModelTest
    public void testExplicitRegistration(KeycloakSession session)
        throws IOException, URISyntaxException, UnparsableException, BadSigningOrEncryptionException {
        Client client = ClientBuilder.newClient();
        String clientId = null;
        try {
//            String st = federationRegistrationExecution(client, session);
//            EntityStatement statement = TrustChainProcessor
//                .parseAndValidateSelfSigned(st);
//            // check entity statements enchancements
//            Assert.assertNotNull(statement.getMetadataPolicy());
//            Assert.assertNotNull(statement.getMetadataPolicy().getRpPolicy());
//            Assert.assertNotNull(statement.getMetadata());
//            Assert.assertNotNull(statement.getMetadata().getRp());
//            clientId = statement.getMetadata().getRp().getClientId();
//            Assert.assertNotNull(clientId);
//
//            // check client exist
//            RealmModel realm = session.realms().getRealmByName("test");
//            ClientModel cl = realm.getClientByClientId(clientId);
//            Assert.assertNotNull(cl);
//            Assert.assertNotNull(cl.getRedirectUris());
//            assertEquals("client RedirectUris size", 1, cl.getRedirectUris().size());
//            assertEquals("https://127.0.0.1:4000/authz_cb/local", cl.getRedirectUris().stream().findFirst().get());
//            Assert.assertTrue(cl.isStandardFlowEnabled());
//            Assert.assertFalse(cl.isImplicitFlowEnabled());
//            Assert.assertFalse(cl.isPublicClient());
//            assertEquals("client-secret", cl.getClientAuthenticatorType());
//            Assert.assertNotNull(cl.getSecret());
        } finally {
            client.close();

        }
    }
    
    @Test 
    public void combineMetadataPolicyInRPStatement() throws IOException, URISyntaxException, MetadataPolicyCombinationException, MetadataPolicyException {
        URL rpEntityStatement = getClass().getClassLoader().getResource("oidc/rpEntityStatement.json");
        byte [] content = Files.readAllBytes(Paths.get(rpEntityStatement.toURI()));
        EntityStatement statement = JsonSerialization.readValue(content, EntityStatement.class);
        URL policyTA = getClass().getClassLoader().getResource("oidc/policyTrustAnchor.json");
        byte [] contentpolicyTA = Files.readAllBytes(Paths.get(policyTA.toURI()));
        RPMetadataPolicy superiorPolicy = JsonSerialization.readValue(contentpolicyTA, RPMetadataPolicy.class);
        URL policyInter = getClass().getClassLoader().getResource("oidc/policyInter.json");
        byte [] contentpolicyInter  = Files.readAllBytes(Paths.get(policyInter.toURI()));
        RPMetadataPolicy inferiorPolicy = JsonSerialization.readValue(contentpolicyInter, RPMetadataPolicy.class);
        superiorPolicy = MetadataPolicyUtils.combineClientPOlicies(superiorPolicy, inferiorPolicy);
        statement = MetadataPolicyUtils.applyPoliciesToRPStatement(statement, superiorPolicy);
        
        //check statement for proper rp policy data
        Assert.assertNotNull(superiorPolicy.getScope());
        Assert.assertNotNull(superiorPolicy.getScope().getSubset_of());
        Assert.assertNames(superiorPolicy.getScope().getSubset_of(), "openid","eduperson");
        Assert.assertNotNull(superiorPolicy.getScope().getSuperset_of());
        Assert.assertNames(superiorPolicy.getScope().getSuperset_of(), "openid");
        assertEquals("openid",superiorPolicy.getScope().getDefaultValue());
        Assert.assertNotNull(superiorPolicy.getApplication_type());
        assertEquals("web",superiorPolicy.getApplication_type().getValue());
        Assert.assertNotNull(superiorPolicy.getContacts());
        Assert.assertNotNull(superiorPolicy.getContacts().getAdd());
        Assert.assertNames(superiorPolicy.getContacts().getAdd(), "helpdesk@org.example.org","helpdesk@federation.example.org");
        Assert.assertNotNull(superiorPolicy.getId_token_signed_response_alg());
        Assert.assertNotNull(superiorPolicy.getId_token_signed_response_alg().getOne_of());
        Assert.assertNames(superiorPolicy.getId_token_signed_response_alg().getOne_of(), "ES384","ES256");
        assertEquals("ES256",superiorPolicy.getId_token_signed_response_alg().getDefaultValue());
        Assert.assertTrue(superiorPolicy.getId_token_signed_response_alg().getEssential());
        
        //check statement for proper rp data
        Assert.assertNotNull(statement.getMetadata());
        Assert.assertNotNull(statement.getMetadata().getRp());
        RPMetadata rp =statement.getMetadata().getRp();
        Assert.assertNotNull(rp.getRedirectUris());
        assertEquals("client RedirectUris size", 1, rp.getRedirectUris().size());
        assertEquals("https://127.0.0.1:4000/authz_cb/local", rp.getRedirectUris().get(0));
        assertEquals("web", rp.getApplicationType());
        Assert.assertNotNull(rp.getResponseTypes());
        assertEquals("ResponseTypes size", 1, rp.getResponseTypes().size());
        assertEquals("code", rp.getResponseTypes().get(0));
        Assert.assertNotNull(rp.getContacts());
        assertEquals("Contacts size", 3, rp.getContacts().size());
        assertContains( rp.getContacts(), "ops@example.com", "helpdesk@org.example.org" , "helpdesk@federation.example.org");
        assertEquals("client_secret_basic", rp.getTokenEndpointAuthMethod());
        assertEquals("ES384", rp.getIdTokenSignedResponseAlg());
        assertEquals("openid", rp.getScope());
        
    }
    
    @Test 
    public void incorrectRPStatement() throws IOException, URISyntaxException, MetadataPolicyCombinationException {
        URL rpEntityStatement = getClass().getClassLoader().getResource("oidc/rpEntityStatement.json");
        byte [] content = Files.readAllBytes(Paths.get(rpEntityStatement.toURI()));
        EntityStatement statement = JsonSerialization.readValue(content, EntityStatement.class);
        //add scope address for being invalid rp metadata
        statement.getMetadata().getRp().setScope("address");
        URL policyTA = getClass().getClassLoader().getResource("oidc/policyTrustAnchor.json");
        byte [] contentpolicyTA = Files.readAllBytes(Paths.get(policyTA.toURI()));
        RPMetadataPolicy superiorPolicy = JsonSerialization.readValue(contentpolicyTA, RPMetadataPolicy.class);
        URL policyInter = getClass().getClassLoader().getResource("oidc/policyInter.json");
        byte [] contentpolicyInter  = Files.readAllBytes(Paths.get(policyInter.toURI()));
        RPMetadataPolicy inferiorPolicy = JsonSerialization.readValue(contentpolicyInter, RPMetadataPolicy.class);
        superiorPolicy = MetadataPolicyUtils.combineClientPOlicies(superiorPolicy, inferiorPolicy);
        
        //check that rp metadata is invalid due to policies
        boolean exceptionThrown = false;
        try {
            statement = MetadataPolicyUtils.applyPoliciesToRPStatement(statement, superiorPolicy);
        } catch (MetadataPolicyException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            exceptionThrown = true;
        }
        Assert.assertTrue("RP metadata must be invalid due to enforcing policies", exceptionThrown);
    }


    private String getOIDCDiscoveryConfiguration(Client client) {
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT))
            .build("test", OIDCFedOPWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        Response response = oidcDiscoveryTarget.request().get();

        return response.readEntity(String.class);
    }

    private String federationRegistrationExecution(Client client,KeycloakSession session) throws IOException, URISyntaxException {
        URI federationRegistrationUri = OIDCFederationResourceProvider
            .federationExplicitRegistration(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test");
        WebTarget oidcDiscoveryTarget = client.target(federationRegistrationUri);
        URL rpEntityStatement = getClass().getClassLoader().getResource("oidc/rpEntityStatement.json");
        byte [] content = Files.readAllBytes(Paths.get(rpEntityStatement.toURI()));
        EntityStatement statement = JsonSerialization.readValue(content, EntityStatement.class);
        statement.setJwks(FedUtils.getKeySet(session));
        String token = session.tokens().encode(statement);

        Response response = oidcDiscoveryTarget.request().post(Entity.text(token));

        return response.readEntity(String.class);
    }

    private void assertContains(List<String> actual, String... expected) {
        for (String exp : expected) {
            Assert.assertTrue(actual.contains(exp));
        }
    }

}
