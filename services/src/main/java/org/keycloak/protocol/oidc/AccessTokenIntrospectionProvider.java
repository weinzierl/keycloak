/*
 *  Copyright 2016 Red Hat, Inc. and/or its affiliates
 *  and other contributors as indicated by the @author tags.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */
package org.keycloak.protocol.oidc;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.io.IOUtils;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.oidc.OIDCIdentityProvider;
import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.common.VerificationException;
import org.keycloak.common.util.Time;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.ClientModel;
import org.keycloak.events.Event;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.events.Details;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.models.customcache.CustomCacheProvider;
import org.keycloak.models.customcache.CustomCacheProviderFactory;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.OIDCIntrospectionMapper;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.services.util.ClientContextUtils;
import org.keycloak.util.JsonSerialization;
import org.keycloak.protocol.oidc.utils.Key;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AccessTokenIntrospectionProvider implements TokenIntrospectionProvider {

    protected final KeycloakSession session;
    private final TokenManager tokenManager;
    protected final RealmModel realm;
    private static final Logger logger = Logger.getLogger(AccessTokenIntrospectionProvider.class);
    private static final String wellKnown = "/.well-known/openid-configuration";
    private static final String PARAM_TOKEN = "token";

    private static CustomCacheProvider tokenRelayCache;

    public AccessTokenIntrospectionProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tokenManager = new TokenManager();
        initTokenCache();
    }

    private void initTokenCache(){
        if(tokenRelayCache != null)
            return;
        CustomCacheProviderFactory factory = (CustomCacheProviderFactory)session.getKeycloakSessionFactory().getProviderFactory(CustomCacheProvider.class, "token-relay-cache");
        if(factory == null)
            throw new NotFoundException("Could not initate TokenRelayCacheProvider. Was not found");
        tokenRelayCache = factory.create(session);
    }

    public Response introspect(String token, EventBuilder eventBuilder) {
        try {
            String[] splitToken = token.split("\\.");
            String accessTokenStr = new String(Base64.getUrlDecoder().decode(splitToken[1]));
            JsonNode tokenJson = new ObjectMapper().readTree(accessTokenStr);
            String issuer = tokenJson.get("iss").asText();
            String realmUrl = Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName());
            if (realmUrl.equals(issuer)) {
                return introspectKeycloak(token, eventBuilder);
            } else {
                long exp = tokenJson.get("exp").asLong();
                if (isExpired(exp)) {
                    String clientId = tokenJson.get("azp").asText();
                    String description = String.format("Token introspection for %s client has expired  token. Token expiration = %d. Current time = %d", clientId, exp, Time.currentTime());
                    logger.warn(description);
                    eventBuilder.detail("Expired token", description);
                    ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
                    tokenMetadata.put("active", false);
                    eventBuilder.error(Errors.INVALID_TOKEN);
                    return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
                }  else {
                    return introspectWithExternal(token, issuer, realm, eventBuilder);
                }
            }

        } catch (Exception e) {
            ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
            tokenMetadata.put("active", false);
            eventBuilder.detail("Failure reason", e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            try {
                return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
            } catch (IOException ioException) {
                throw new RuntimeException("Error creating token introspection response.", e);
            }
        }
    }

    protected Response introspectKeycloak (String token, EventBuilder eventBuilder) {
        AccessToken accessToken = null;
        try {

            accessToken = verifyAccessToken(token, eventBuilder);
            ObjectNode tokenMetadata;

            if (accessToken != null) {
                tokenMetadata = JsonSerialization.createObjectNode(accessToken);
                tokenMetadata.put("client_id", accessToken.getIssuedFor());

                if (!tokenMetadata.has("username")) {
                    if (accessToken.getPreferredUsername() != null) {
                        tokenMetadata.put("username", accessToken.getPreferredUsername());
                    } else {
                        UserModel userModel = session.users().getUserById(realm, accessToken.getSubject());
                        if (userModel != null) {
                            tokenMetadata.put("username", userModel.getUsername());
                        }
                    }
                }
            } else {
                tokenMetadata = JsonSerialization.createObjectNode();
                logger.warn("Keycloak token introspection return null access token.");
                eventBuilder.detail("AccessToken verification", "Verification returned null access token");
                eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            }

            tokenMetadata.put("active", accessToken != null);
            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            String clientId = accessToken != null ? accessToken.getIssuedFor() : "unknown";
            logger.warn("Exception during Keycloak introspection for "+clientId+" client.",e);
            eventBuilder.detail("introspection failure", e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    protected AccessToken verifyAccessToken(String token, EventBuilder eventBuilder) {
        AccessToken accessToken;

        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(token, AccessToken.class)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            accessToken = verifier.verify().getToken();
        } catch (VerificationException e) {
            eventBuilder.detail("Access token verification failed", e.getMessage());
            logger.warnf("Introspection access token : JWT check failed: %s", e.getMessage());
            return null;
        }

        RealmModel realm = this.session.getContext().getRealm();

        return transformAccessToken(accessToken, tokenManager.checkTokenValidForIntrospection(session, realm, accessToken, false, eventBuilder), eventBuilder);
    }

    /**
     * Produce AccessToken for introspection based on AccessToken provided and protocol mappers for introspection.
     * Return null if AccessToken is not valid for introspection ( user == null)
     * @param token access token
     * @param user from token
     * @return
     */
    private AccessToken transformAccessToken(AccessToken token, UserModel user, EventBuilder eventBuilder) {

        if (  user == null) {
            eventBuilder.detail("AccessToken user", "Failed to identify user from access token");
            return null;
        }

        //client - user exist - otherwise validation failed before
        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        ClientContextUtils ccu= new ClientContextUtils(client,token.getScope(),session,user);

        AtomicReference<AccessToken> finalToken = new AtomicReference<>(token);
        //must find another way
       ccu.getProtocolMappersStream().flatMap(mapperModel -> {
            ProtocolMapper mapper = (ProtocolMapper) session.getKeycloakSessionFactory().getProviderFactory(ProtocolMapper.class, mapperModel.getProtocolMapper());
            if (mapper == null)
                return null;
            Map<ProtocolMapperModel, ProtocolMapper> protocolMapperMap = new HashMap<>();
            protocolMapperMap.put(mapperModel, mapper);
            return protocolMapperMap.entrySet().stream();
        }).filter(Objects::nonNull)
                .sorted(Comparator.comparing(ProtocolMapperUtils::compare))
                .filter(mapper -> mapper.getValue() instanceof OIDCIntrospectionMapper)
                .forEach(mapper -> finalToken.set(((OIDCIntrospectionMapper) mapper.getValue())
                        .transformAccessTokenForIntrospection(finalToken.get(), mapper.getKey(), user)));
        return finalToken.get();
    }

  protected Response introspectWithExternal(String token, String issuer, RealmModel realm, EventBuilder eventBuilder) {

        try {
            String cachedToken = (String) tokenRelayCache.get(new Key(token, realm.getName()));
            if (cachedToken != null)
                return Response.ok(cachedToken).type(MediaType.APPLICATION_JSON_TYPE).build();

            IdentityProviderModel issuerIdp = realm.getIdentityProvidersStream().filter(idp -> issuer.equals(idp.getConfig().get("issuer"))).findAny().orElse(null);
            if (issuerIdp != null) {
                OIDCIdentityProviderConfig oidcIssuerIdp = new OIDCIdentityProviderConfig(issuerIdp);
                OIDCIdentityProvider oidcIssuerProvider = new OIDCIdentityProvider(session, oidcIssuerIdp);
                InputStream inputStream = session.getProvider(HttpClientProvider.class).get(new String(oidcIssuerIdp.getIssuer() + wellKnown));
                OIDCConfigurationRepresentation rep = JsonSerialization.readValue(inputStream, OIDCConfigurationRepresentation.class);
                if (rep.getIntrospectionEndpoint() != null) {
                    SimpleHttp.Response response = oidcIssuerProvider.authenticateTokenRequest(SimpleHttp.doPost(rep.getIntrospectionEndpoint(), session).param(PARAM_TOKEN, token)).asResponse();
                    if (response.getResponse().getStatusLine().getStatusCode() > 300) {
                        logger.warn("Remote introspection Idp return http status " + response.getResponse().getStatusLine().getStatusCode() + " with body :");
                        logger.warn(IOUtils.toString(response.getResponse().getEntity().getContent(), StandardCharsets.UTF_8));
                        ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
                        tokenMetadata.put("active", false);
                        return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
                    }
                    String responseJson = IOUtils.toString(response.getResponse().getEntity().getContent(), Charset.defaultCharset());
                    tokenRelayCache.put(new Key(token, realm.getName()), responseJson);
                    return Response.status(response.getResponse().getStatusLine().getStatusCode()).type(MediaType.APPLICATION_JSON_TYPE).entity(responseJson).build();
                }
            }
            //if failed to find issuer in IdPs or IntrospectionEndpoint does not exist for specific Idp return false
            String problem = issuerIdp != null ? "Remote introspection: problem getting remote Idp with issuer " + issuer + "introspection endpoint" : "Remote introspection: Idp with issuer " + issuer + " does not exist";
            logger.warn(problem);
            eventBuilder.detail("Remote introspection problem", problem);
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            ObjectNode tokenMetadata = JsonSerialization.createObjectNode();
            tokenMetadata.put("active", false);
            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            eventBuilder.detail("Remote introspection exception", e.getMessage());
            eventBuilder.error(Errors.TOKEN_INTROSPECTION_FAILED);
            logger.warn("Error during remote introspection", e);
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }
    private boolean isExpired(Long exp) {
        return exp != null && exp != 0 ? Time.currentTime() > exp : false;
    }


    @Override
    public void close() {

    }
}
