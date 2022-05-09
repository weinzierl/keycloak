/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.testsuite.oidc;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.OAuth2Constants;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.Profile;
import org.keycloak.crypto.Algorithm;
import org.keycloak.jose.jwe.JWEConstants;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.Constants;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolFactory;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCWellKnownProviderFactory;
import org.keycloak.protocol.oidc.representations.MTLSEndpointAliases;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.protocol.oidc.utils.OIDCResponseType;
import org.keycloak.representations.IDToken;
import org.keycloak.representations.idm.ClientScopeRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.services.clientregistration.ClientRegistrationService;
import org.keycloak.services.clientregistration.oidc.OIDCClientRegistrationProviderFactory;
import org.keycloak.services.resources.Cors;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.admin.AbstractAdminTest;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.arquillian.annotation.EnableFeature;
import org.keycloak.testsuite.forms.BrowserFlowTest;
import org.keycloak.testsuite.forms.LevelOfAssuranceFlowTest;
import org.keycloak.testsuite.util.AdminClientUtil;
import org.keycloak.testsuite.util.ClientManager;
import org.keycloak.testsuite.util.OAuthClient;
import org.keycloak.testsuite.util.TokenSignatureUtil;
import org.keycloak.testsuite.wellknown.CustomOIDCWellKnownProviderFactory;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer.REMOTE;

/**
 * @author <a href="mailto:mposolda@redhat.com">Marek Posolda</a>
 */
public class OIDCWellKnownProviderTest extends AbstractKeycloakTest {

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        RealmRepresentation realm = AbstractAdminTest.loadJson(getClass().getResourceAsStream("/testrealm.json"), RealmRepresentation.class);
        testRealms.add(realm);
    }

    @Before
    public void clientConfiguration() {
        ClientManager.realm(adminClient.realm("test")).clientId("test-app").directAccessGrant(true);
        /*
         * Configure the default client ID. Seems like OAuthClient is keeping the state of clientID
         * For example: If some test case configure oauth.clientId("sample-public-client"), other tests
         * will faile and the clientID will always be "sample-public-client
         * @see AccessTokenTest#testAuthorizationNegotiateHeaderIgnored()
         */
        oauth.clientId("test-app");
    }

    @Test
    public void testDiscovery() {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);

            // URIs are filled
            assertEquals(oidcConfig.getAuthorizationEndpoint(), OIDCLoginProtocolService.authUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            assertEquals(oidcConfig.getTokenEndpoint(), oauth.getAccessTokenUrl());
            assertEquals(oidcConfig.getUserinfoEndpoint(), OIDCLoginProtocolService.userInfoUrl(UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT)).build("test").toString());
            assertEquals(oidcConfig.getJwksUri(), oauth.getCertsUrl("test"));


            String registrationUri = UriBuilder
                    .fromUri(OAuthClient.AUTH_SERVER_ROOT)
                    .path(RealmsResource.class)
                    .path(RealmsResource.class, "getClientsService")
                    .path(ClientRegistrationService.class, "provider")
                    .build("test", OIDCClientRegistrationProviderFactory.ID)
                    .toString();
            assertEquals(oidcConfig.getRegistrationEndpoint(), registrationUri);

            // Support standard + implicit + hybrid flow
            assertContains(oidcConfig.getResponseTypesSupported(), OAuth2Constants.CODE, OIDCResponseType.ID_TOKEN, "id_token token", "code id_token", "code token", "code id_token token");
            assertEquals(oidcConfig.getGrantTypesSupported().size(),7);
            assertContains(oidcConfig.getGrantTypesSupported(), OAuth2Constants.AUTHORIZATION_CODE, OAuth2Constants.IMPLICIT,
                OAuth2Constants.DEVICE_CODE_GRANT_TYPE);
            assertContains(oidcConfig.getResponseModesSupported(), "query", "fragment", "form_post", "jwt", "query.jwt", "fragment.jwt", "form_post.jwt");

            Assert.assertNames(oidcConfig.getSubjectTypesSupported(), "pairwise", "public");

            // Signature algorithms
            Assert.assertNames(oidcConfig.getIdTokenSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);
            Assert.assertNames(oidcConfig.getUserInfoSigningAlgValuesSupported(), "none", Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);
            Assert.assertNames(oidcConfig.getRequestObjectSigningAlgValuesSupported(), "none", Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);
            Assert.assertNames(oidcConfig.getAuthorizationSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);

            // request object encryption algorithms
            Assert.assertNames(oidcConfig.getRequestObjectEncryptionAlgValuesSupported(), JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256, JWEConstants.RSA1_5);
            Assert.assertNames(oidcConfig.getRequestObjectEncryptionEncValuesSupported(), JWEConstants.A256GCM, JWEConstants.A192GCM, JWEConstants.A128GCM, JWEConstants.A128CBC_HS256, JWEConstants.A192CBC_HS384, JWEConstants.A256CBC_HS512);

            // Encryption algorithms
            Assert.assertNames(oidcConfig.getIdTokenEncryptionAlgValuesSupported(), JWEConstants.RSA1_5, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256);
            Assert.assertNames(oidcConfig.getIdTokenEncryptionEncValuesSupported(), JWEConstants.A128CBC_HS256, JWEConstants.A128GCM, JWEConstants.A192CBC_HS384, JWEConstants.A192GCM, JWEConstants.A256CBC_HS512, JWEConstants.A256GCM);
            Assert.assertNames(oidcConfig.getAuthorizationEncryptionAlgValuesSupported(), JWEConstants.RSA1_5, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256);
            Assert.assertNames(oidcConfig.getAuthorizationEncryptionEncValuesSupported(), JWEConstants.A128CBC_HS256, JWEConstants.A128GCM, JWEConstants.A192CBC_HS384, JWEConstants.A192GCM, JWEConstants.A256CBC_HS512, JWEConstants.A256GCM);
            Assert.assertNames(oidcConfig.getUserInfoEncryptionAlgValuesSupported(), JWEConstants.RSA1_5, JWEConstants.RSA_OAEP, JWEConstants.RSA_OAEP_256);
            Assert.assertNames(oidcConfig.getUserInfoEncryptionEncValuesSupported(), JWEConstants.A128CBC_HS256, JWEConstants.A128GCM, JWEConstants.A192CBC_HS384, JWEConstants.A192GCM, JWEConstants.A256CBC_HS512, JWEConstants.A256GCM);

            // Client authentication
            Assert.assertNames(oidcConfig.getTokenEndpointAuthMethodsSupported(), "client_secret_basic", "client_secret_post", "private_key_jwt", "client_secret_jwt", "tls_client_auth");
            Assert.assertNames(oidcConfig.getTokenEndpointAuthSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);
            // NOTE: Those are overriden in "oidc-well-known-config-override.json" and they are tested in testDefaultProviderCustomizations
            //Assert.assertNames(oidcConfig.getIntrospectionEndpointAuthMethodsSupported(), "private_key_jwt", "client_secret_jwt", "tls_client_auth", "custom_nonexisting_authenticator");
            Assert.assertNames(oidcConfig.getIntrospectionEndpointAuthSigningAlgValuesSupported(), Algorithm.PS256,
                Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256,
                Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);

            // Claims
            Assert.assertNames(oidcConfig.getClaimsSupported(), RepresentationToModel.DEFAULT_CLAIMS_SUPPORTED.toArray(new String[RepresentationToModel.DEFAULT_CLAIMS_SUPPORTED.size()]));
            Assert.assertNames(oidcConfig.getClaimTypesSupported(), "normal");
            Assert.assertTrue(oidcConfig.getClaimsParameterSupported());

            // Scopes supported
            List<String> expectedScopeList = Stream.of(OAuth2Constants.SCOPE_OPENID, OAuth2Constants.OFFLINE_ACCESS,
                    OAuth2Constants.SCOPE_PROFILE, OAuth2Constants.SCOPE_EMAIL, OAuth2Constants.SCOPE_PHONE, OAuth2Constants.SCOPE_ADDRESS, OIDCLoginProtocolFactory.ACR_SCOPE,
                    OIDCLoginProtocolFactory.ROLES_SCOPE, OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE, OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE).collect(Collectors.toList());
            assertScopesSupportedMatchesWithRealm(oidcConfig, expectedScopeList);

            // Request and Request_Uri
            Assert.assertTrue(oidcConfig.getRequestParameterSupported());
            Assert.assertTrue(oidcConfig.getRequestUriParameterSupported());
            Assert.assertTrue(oidcConfig.getRequireRequestUriRegistration());

            // KEYCLOAK-7451 OAuth Authorization Server Metadata for Proof Key for Code Exchange
            // PKCE support
            Assert.assertNames(oidcConfig.getCodeChallengeMethodsSupported(), OAuth2Constants.PKCE_METHOD_PLAIN, OAuth2Constants.PKCE_METHOD_S256);

            // KEYCLOAK-6771 Certificate Bound Token
            // https://tools.ietf.org/html/draft-ietf-oauth-mtls-08#section-6.2
            Assert.assertTrue(oidcConfig.getTlsClientCertificateBoundAccessTokens());
            MTLSEndpointAliases mtlsEndpointAliases = oidcConfig.getMtlsEndpointAliases();
            Assert.assertEquals(oidcConfig.getTokenEndpoint(), mtlsEndpointAliases.getTokenEndpoint());
            Assert.assertEquals(oidcConfig.getRevocationEndpoint(), mtlsEndpointAliases.getRevocationEndpoint());

            // CIBA
            assertEquals(oidcConfig.getBackchannelAuthenticationEndpoint(), oauth.getBackchannelAuthenticationUrl());
            assertContains(oidcConfig.getGrantTypesSupported(), OAuth2Constants.CIBA_GRANT_TYPE);
            Assert.assertNames(oidcConfig.getBackchannelTokenDeliveryModesSupported(), "poll", "ping");
            Assert.assertNames(oidcConfig.getBackchannelAuthenticationRequestSigningAlgValuesSupported(), Algorithm.PS256, Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256, Algorithm.ES384, Algorithm.ES512);

            Assert.assertTrue(oidcConfig.getBackchannelLogoutSupported());
            Assert.assertTrue(oidcConfig.getBackchannelLogoutSessionSupported());

            // Token Revocation
            assertEquals(oidcConfig.getRevocationEndpoint(), oauth.getTokenRevocationUrl());
            Assert.assertNames(oidcConfig.getRevocationEndpointAuthMethodsSupported(), "client_secret_basic",
                "client_secret_post", "private_key_jwt", "client_secret_jwt", "tls_client_auth");
            Assert.assertNames(oidcConfig.getRevocationEndpointAuthSigningAlgValuesSupported(), Algorithm.PS256,
                Algorithm.PS384, Algorithm.PS512, Algorithm.RS256, Algorithm.RS384, Algorithm.RS512, Algorithm.ES256,
                Algorithm.ES384, Algorithm.ES512, Algorithm.HS256, Algorithm.HS384, Algorithm.HS512);

            assertEquals(oidcConfig.getDeviceAuthorizationEndpoint(), oauth.getDeviceAuthorizationUrl());

            // Pushed Authorization Request (PAR)
            assertEquals(oauth.getParEndpointUrl(), oidcConfig.getPushedAuthorizationRequestEndpoint());
            assertEquals(Boolean.FALSE, oidcConfig.getRequirePushedAuthorizationRequests());

            // frontchannel logout
            assertTrue(oidcConfig.getFrontChannelLogoutSessionSupported());
            assertTrue(oidcConfig.getFrontChannelLogoutSupported());

        } finally {
            client.close();
        }
    }

    @Test
    public void testHttpDiscovery() {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, "http://localhost:8180/auth");

            Assert.assertNotNull(oidcConfig.getJwksUri());

            // Token Revocation
            Assert.assertNotNull(oidcConfig.getRevocationEndpoint());
            Assert.assertNotNull(oidcConfig.getRevocationEndpointAuthMethodsSupported());
            Assert.assertNotNull(oidcConfig.getRevocationEndpointAuthSigningAlgValuesSupported());
        } finally {
            client.close();
        }
    }

    @Test
    public void testIssuerMatches() throws Exception {
        OAuthClient.AuthorizationEndpointResponse authzResp = oauth.doLogin("test-user@localhost", "password");
        OAuthClient.AccessTokenResponse response = oauth.doAccessTokenRequest(authzResp.getCode(), "password");
        assertEquals(200, response.getStatusCode());
        IDToken idToken = oauth.verifyIDToken(response.getIdToken());

        Client client = AdminClientUtil.createResteasyClient();
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);

            // assert issuer matches
            assertEquals(idToken.getIssuer(), oidcConfig.getIssuer());
        } finally {
            client.close();
        }
    }

    @Test
    public void corsTest() {
        Client client = AdminClientUtil.createResteasyClient();
        UriBuilder builder = UriBuilder.fromUri(OAuthClient.AUTH_SERVER_ROOT);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", OIDCWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);


        Invocation.Builder request = oidcDiscoveryTarget.request();
        request.header(Cors.ORIGIN_HEADER, "http://somehost");
        Response response = request.get();

        assertEquals("http://somehost", response.getHeaders().getFirst(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));


        Invocation.Builder nullRequest = oidcDiscoveryTarget.request();
        nullRequest.header(Cors.ORIGIN_HEADER, "null");
        Response nullResponse = nullRequest.get();

        assertEquals("null", nullResponse.getHeaders().getFirst(Cors.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    public void certs() throws IOException {
        TokenSignatureUtil.registerKeyProvider(Algorithm.ES256, adminClient, testContext);

        OIDCConfigurationRepresentation representation = SimpleHttp.doGet(getAuthServerRoot().toString() + "realms/test/.well-known/openid-configuration", client).asJson(OIDCConfigurationRepresentation.class);
        String jwksUri = representation.getJwksUri();

        JSONWebKeySet jsonWebKeySet = SimpleHttp.doGet(jwksUri, client).asJson(JSONWebKeySet.class);
        assertEquals(3, jsonWebKeySet.getKeys().length);
    }

    @Test
    public void testIntrospectionEndpointClaim() throws IOException {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            ObjectNode oidcConfig = JsonSerialization
                .readValue(getOIDCDiscoveryConfiguration(client, OAuthClient.AUTH_SERVER_ROOT), ObjectNode.class);
            assertEquals(oidcConfig.get("introspection_endpoint").asText(),
                getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT).getIntrospectionEndpoint());
        } finally {
            client.close();
        }
    }

    @Test
    @AuthServerContainerExclude(REMOTE)
    public void testAcrValuesSupported() throws IOException {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            // Default values when no "acr-to-loa" mapping and no authentication flow configured
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "0", "1");

            // Update authentication flow and see it uses "acr" values from it
            LevelOfAssuranceFlowTest.configureStepUpFlow(testingClient);
            oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "0", "1", "2", "3");

            // Configure "ACR-To-Loa" mapping and check it has both configured values and numbers from authentication flow
            RealmResource testRealm = adminClient.realm("test");
            RealmRepresentation realmRep = testRealm.toRepresentation();
            Map<String, Integer> acrToLoa = new HashMap<>();
            acrToLoa.put("poor", 0);
            acrToLoa.put("silver", 1);
            acrToLoa.put("gold", 2);
            String acrToLoaAttr = JsonSerialization.writeValueAsString(acrToLoa);
            realmRep.getAttributes().put(Constants.ACR_LOA_MAP, acrToLoaAttr);
            testRealm.update(realmRep);

            oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "poor", "silver", "gold", "0", "1", "2", "3");

            // Use mappings even with values not included in the authentication flow
            acrToLoa = new HashMap<>();
            acrToLoa.put("poor", 0);
            acrToLoa.put("silver", 1);
            acrToLoa.put("gold", 2);
            acrToLoa.put("platinum", 3);
            acrToLoa.put("diamond", 4);
            acrToLoaAttr = JsonSerialization.writeValueAsString(acrToLoa);
            realmRep.getAttributes().put(Constants.ACR_LOA_MAP, acrToLoaAttr);
            testRealm.update(realmRep);

            oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            Assert.assertNames(oidcConfig.getAcrValuesSupported(), "poor", "silver", "gold", "platinum", "diamond", "0", "1", "2", "3");

            // Revert realm and flow
            realmRep.getAttributes().remove(Constants.ACR_LOA_MAP);
            testRealm.update(realmRep);
            BrowserFlowTest.revertFlows(testRealm, "browser -  Level of Authentication FLow");
        } finally {
            client.close();
        }
    }

    @Test
    @AuthServerContainerExclude(REMOTE)
    public void testDefaultProviderCustomizations() throws IOException {
        Client client = AdminClientUtil.createResteasyClient();
        String showScopeId = null;
        String hideScopeId = null;
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);

            // Assert that CustomOIDCWellKnownProvider was used as a prioritized provider over default OIDCWellKnownProvider
            MTLSEndpointAliases mtlsEndpointAliases = oidcConfig.getMtlsEndpointAliases();
            Assert.assertEquals("https://placeholder-host-set-by-testsuite-provider/registration", mtlsEndpointAliases.getRegistrationEndpoint());
            Assert.assertEquals("bar", oidcConfig.getOtherClaims().get("foo"));

            // Assert some configuration was overriden
            Assert.assertEquals("some-new-property-value", oidcConfig.getOtherClaims().get("some-new-property"));
            Assert.assertEquals("nested-value", ((Map) oidcConfig.getOtherClaims().get("some-new-property-compound")).get("nested1"));
            Assert.assertNames(oidcConfig.getIntrospectionEndpointAuthMethodsSupported(), "private_key_jwt", "client_secret_jwt", "tls_client_auth", "custom_nonexisting_authenticator");

            List<String> expectedScopeList = Stream.of(OAuth2Constants.SCOPE_OPENID, OAuth2Constants.OFFLINE_ACCESS,
                    OAuth2Constants.SCOPE_PROFILE, OAuth2Constants.SCOPE_EMAIL, OAuth2Constants.SCOPE_PHONE, OAuth2Constants.SCOPE_ADDRESS, OIDCLoginProtocolFactory.ACR_SCOPE,
                    OIDCLoginProtocolFactory.ROLES_SCOPE, OIDCLoginProtocolFactory.WEB_ORIGINS_SCOPE, OIDCLoginProtocolFactory.MICROPROFILE_JWT_SCOPE).collect(Collectors.toList());
            // Exact names already tested in OIDC
            assertScopesSupportedMatchesWithRealm(oidcConfig, expectedScopeList);

            //create 2 client scope - one with hideFromOpenIDProviderMetadata equal to true
            ClientScopeRepresentation clientScope = new ClientScopeRepresentation();
            clientScope.setName("show-scope");
            clientScope.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            Response resp = adminClient.realm("test").clientScopes().create(clientScope);
            showScopeId = ApiUtil.getCreatedId(resp);
            resp.close();

            ClientScopeRepresentation clientScope2 = new ClientScopeRepresentation();
            clientScope2.setName("hidden-scope");
            clientScope2.setProtocol(OIDCLoginProtocol.LOGIN_PROTOCOL);
            Map<String,String> attributes = new HashMap<>();
            attributes.put(ClientScopeModel.HIDE_FROM_OPENID_PROVIDER_METADATA,"true");
            clientScope2.setAttributes(attributes);
            Response resp2 = adminClient.realm("test").clientScopes().create(clientScope2);
            hideScopeId = ApiUtil.getCreatedId(resp2);
            resp2.close();

            expectedScopeList.add("show-scope");
            oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            assertScopesSupportedMatchesWithRealm(oidcConfig, expectedScopeList);

            // Temporarily disable client scopes
            getTestingClient().testing().setSystemPropertyOnServer(CustomOIDCWellKnownProviderFactory.INCLUDE_CLIENT_SCOPES, "false");
            oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            Assert.assertNull(oidcConfig.getScopesSupported());
        } finally {
            getTestingClient().testing().setSystemPropertyOnServer(CustomOIDCWellKnownProviderFactory.INCLUDE_CLIENT_SCOPES, null);
            if ( showScopeId != null)
                adminClient.realm("test").clientScopes().get(showScopeId).remove();
            if ( hideScopeId != null)
                adminClient.realm("test").clientScopes().get(hideScopeId).remove();
            client.close();
        }
    }

    @Test
    @EnableFeature(value = Profile.Feature.TOKEN_EXCHANGE, skipRestart = true)
    public void testGrantTypesSupportedWithTokenExchange() throws IOException {
        Client client = AdminClientUtil.createResteasyClient();
        try {
            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            assertEquals(oidcConfig.getGrantTypesSupported().size(),8);
            assertContains(oidcConfig.getGrantTypesSupported(), OAuth2Constants.TOKEN_EXCHANGE_GRANT_TYPE);
        } finally {
            client.close();
        }
    }

    @Test
    @AuthServerContainerExclude(REMOTE)
    public void testChangeClaimsSupported() throws IOException {
        Client client = AdminClientUtil.createResteasyClient();
        RealmResource testRealm = adminClient.realm("test");
        RealmRepresentation realmRep = testRealm.toRepresentation();
        try {
            realmRep.setClaimsSupported(Stream.of("aud", "sub", "iss", IDToken.AUTH_TIME, IDToken.NAME, IDToken.GIVEN_NAME, IDToken.FAMILY_NAME, IDToken.PREFERRED_USERNAME, IDToken.EMAIL, IDToken.ACR,"email_verified").collect(Collectors.toList()));
            testRealm.update(realmRep);

            OIDCConfigurationRepresentation oidcConfig = getOIDCDiscoveryRepresentation(client, OAuthClient.AUTH_SERVER_ROOT);
            Assert.assertNames(oidcConfig.getClaimsSupported(), "aud", "sub", "iss", IDToken.AUTH_TIME, IDToken.NAME, IDToken.GIVEN_NAME, IDToken.FAMILY_NAME, IDToken.PREFERRED_USERNAME, IDToken.EMAIL, IDToken.ACR,"email_verified");

            realmRep.setClaimsSupported(RepresentationToModel.DEFAULT_CLAIMS_SUPPORTED.stream().collect(Collectors.toList()));
            testRealm.update(realmRep);
        } finally {
            client.close();
        }
    }

    private void assertScopesSupportedMatchesWithRealm(OIDCConfigurationRepresentation oidcConfig, List<String> expectedScopeList) {
        Assert.assertNames(oidcConfig.getScopesSupported(), expectedScopeList.toArray(new String[expectedScopeList.size()]) );
    }

    private OIDCConfigurationRepresentation getOIDCDiscoveryRepresentation(Client client, String uriTemplate) {
        try {
            return JsonSerialization.readValue(getOIDCDiscoveryConfiguration(client, uriTemplate), OIDCConfigurationRepresentation.class);
        } catch (IOException cause) {
            throw new RuntimeException("Failed to parse OIDC configuration", cause);
        }
    }

    private String getOIDCDiscoveryConfiguration(Client client, String uriTemplate) {
        UriBuilder builder = UriBuilder.fromUri(uriTemplate);
        URI oidcDiscoveryUri = RealmsResource.wellKnownProviderUrl(builder).build("test", OIDCWellKnownProviderFactory.PROVIDER_ID);
        WebTarget oidcDiscoveryTarget = client.target(oidcDiscoveryUri);

        Response response = oidcDiscoveryTarget.request().get();

        assertEquals("no-cache, must-revalidate, no-transform, no-store", response.getHeaders().getFirst("Cache-Control"));

        return response.readEntity(String.class);
    }

    private void assertContains(List<String> actual, String... expected) {
        for (String exp : expected) {
            Assert.assertTrue(actual.contains(exp));
        }
    }
}
