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

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jboss.logging.Logger;
import org.keycloak.TokenVerifier;
import org.keycloak.common.VerificationException;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.crypto.SignatureVerifierContext;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.UserSessionModel;
import org.keycloak.protocol.ProtocolMapper;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.mappers.OIDCIntrospectionMapper;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.UserSessionCrossDCManager;
import org.keycloak.services.util.ClientContextUtils;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * @author <a href="mailto:psilva@redhat.com">Pedro Igor</a>
 */
public class AccessTokenIntrospectionProvider implements TokenIntrospectionProvider {

    private final KeycloakSession session;
    private final TokenManager tokenManager;
    private final RealmModel realm;
    private static final Logger logger = Logger.getLogger(AccessTokenIntrospectionProvider.class);

    public AccessTokenIntrospectionProvider(KeycloakSession session) {
        this.session = session;
        this.realm = session.getContext().getRealm();
        this.tokenManager = new TokenManager();
    }

    public Response introspect(String token) {
        try {
            AccessToken accessToken = verifyAccessToken(token);
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
            }

            tokenMetadata.put("active", accessToken != null);

            return Response.ok(JsonSerialization.writeValueAsBytes(tokenMetadata)).type(MediaType.APPLICATION_JSON_TYPE).build();
        } catch (Exception e) {
            throw new RuntimeException("Error creating token introspection response.", e);
        }
    }

    protected AccessToken verifyAccessToken(String token) {
        AccessToken accessToken;

        try {
            TokenVerifier<AccessToken> verifier = TokenVerifier.create(token, AccessToken.class)
                    .realmUrl(Urls.realmIssuer(session.getContext().getUri().getBaseUri(), realm.getName()));

            SignatureVerifierContext verifierContext = session.getProvider(SignatureProvider.class, verifier.getHeader().getAlgorithm().name()).verifier(verifier.getHeader().getKeyId());
            verifier.verifierContext(verifierContext);

            accessToken = verifier.verify().getToken();
        } catch (VerificationException e) {
            logger.debugf("JWT check failed: %s", e.getMessage());
            return null;
        }

        RealmModel realm = this.session.getContext().getRealm();

        return  transformAccessToken(accessToken,  tokenManager.checkTokenValidForIntrospection(session, realm, accessToken, false)) ;
    }

    /**
     * Produce AccessToken for introspection based on AccessToken provided and protocol mappers for introspection.
     * Return null if AccessToken is not valid for introspection ( user == null)
     * @param token access token
     * @param user from token
     * @return
     */
    private AccessToken transformAccessToken(AccessToken token, UserModel user) {

        if (  user == null)
            return null;

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

    @Override
    public void close() {

    }
}
