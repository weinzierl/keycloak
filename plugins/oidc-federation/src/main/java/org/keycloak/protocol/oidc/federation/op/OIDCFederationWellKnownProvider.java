package org.keycloak.protocol.oidc.federation.op;
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



import org.keycloak.TokenCategory;
import org.keycloak.common.util.Time;
import org.keycloak.crypto.HashProvider;
import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.crypto.SignatureProvider;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.jose.jws.crypto.HashUtils;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCLoginProtocol;
import org.keycloak.protocol.oidc.OIDCLoginProtocolService;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.federation.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.beans.OIDCFederationConfigurationRepresentation;
import org.keycloak.protocol.oidc.federation.beans.OPMetadata;
import org.keycloak.protocol.oidc.federation.exceptions.InternalServerErrorException;
import org.keycloak.protocol.oidc.federation.exceptions.NoAlgorithmException;
import org.keycloak.protocol.oidc.federation.rest.OIDCFederationResourceProvider;
import org.keycloak.protocol.oidc.federation.rest.OIDCFederationResourceProviderFactory;
import org.keycloak.protocol.oidc.federation.rest.op.FederationOPService;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;


public class OIDCFederationWellKnownProvider extends OIDCWellKnownProvider {

	public static final Long ENTITY_EXPIRES_AFTER_SEC = 86400L; //24 hours
	public static final List<String> CLIENT_REGISTRATION_TYPES_SUPPORTED = Arrays.asList("automatic", "explicit");
	
	
    private KeycloakSession session;

    public OIDCFederationWellKnownProvider(KeycloakSession session) {
    	super(session);
        this.session = session;
    }

    @Override
    public Object getConfig() {
    	    	
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);

        RealmModel realm = session.getContext().getRealm();

        UriBuilder frontendUriBuilder = RealmsResource.realmBaseUrl(frontendUriInfo);
        UriBuilder backendUriBuilder = RealmsResource.realmBaseUrl(backendUriInfo);
        
    	OIDCFederationConfigurationRepresentation config;
		try {
			config = from(((OIDCConfigurationRepresentation) super.getConfig()));
		} catch (IOException e) {
			throw new InternalServerErrorException("Could not form the configuration response");
		} 
		
        //additional federation-specific configuration
        config.setFederationRegistrationEndpoint(backendUriBuilder.clone().path(OIDCFederationResourceProviderFactory.ID).path(OIDCFederationResourceProvider.class, "getFederationOPService").path(FederationOPService.class, "getFederationRegistration").build(realm.getName()).toString());
        config.setPushedAuthorizationRequestEndpoint(backendUriBuilder.clone().path(OIDCFederationResourceProviderFactory.ID).path(OIDCFederationResourceProvider.class, "getFederationOPService").path(FederationOPService.class, "postPushedAuthorization").build(realm.getName()).toString());
        config.setClientRegistrationTypesSupported(CLIENT_REGISTRATION_TYPES_SUPPORTED);
//        config.setClientRegistrationAuthnMethodsSupported(clientRegistrationAuthnMethodsSupported);
        
		OPMetadata metadata = new OPMetadata();
		metadata.setMetadata(config);
		
		
        EntityStatement entityStatement = new EntityStatement();
        entityStatement.issuedFor(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));
        entityStatement.setMetadata(metadata);
//        entityStatement.setAuthorityHints(authorityHints);
        entityStatement.setJwks(getKeySet());
        entityStatement.issuer(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));
        entityStatement.issuedNow();
        entityStatement.exp(Long.valueOf(Time.currentTime()) + ENTITY_EXPIRES_AFTER_SEC);
        	
        //sign and encode entity statement
        String encodedToken = session.tokens().encode(entityStatement);
        
        return encodedToken;
    }

    @Override
    public void close() {
    }

    
    private JSONWebKeySet getKeySet() {
    	List<JWK> keys = new LinkedList<>();
        for (KeyWrapper k : session.keys().getKeys(session.getContext().getRealm())) {
            if (k.getStatus().isEnabled() && k.getUse().equals(KeyUse.SIG) && k.getPublicKey() != null) {
                JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithm());
                if (k.getType().equals(KeyType.RSA)) {
                    keys.add(b.rsa(k.getPublicKey(), k.getCertificate()));
                } else if (k.getType().equals(KeyType.EC)) {
                    keys.add(b.ec(k.getPublicKey()));
                }
            }
        }

        JSONWebKeySet keySet = new JSONWebKeySet();

        JWK[] k = new JWK[keys.size()];
        k = keys.toArray(k);
        keySet.setKeys(k);
        return keySet;
    }
    
    public static OIDCFederationConfigurationRepresentation from(OIDCConfigurationRepresentation representation) throws IOException {
    	return JsonSerialization.readValue(JsonSerialization.writeValueAsString(representation), OIDCFederationConfigurationRepresentation.class);
    }

    
//    private List<String> getAvailableAsymSigAlgTypes(){
//    	return session.keys().getKeys(session.getContext().getRealm()).stream()
//    			.filter(key -> key.getUse().equals(KeyUse.SIG) && key.getType().equals(KeyType.RSA))
//    			.map(key -> key.getAlgorithm())
//    			.collect(Collectors.toList());
//    }
//    
//    
//    
//	private String generateOIDCHash(String input) throws NoAlgorithmException {
//		String signatureAlgorithm = getAvailableAsymSigAlgTypes().stream().findFirst().orElseThrow(() -> new NoAlgorithmException("No available asymmetric key signing algorithm available for realm "+session.getContext().getRealm().getName()));
//		
//        SignatureProvider signatureProvider = session.getProvider(SignatureProvider.class, signatureAlgorithm);
//        String hashAlgorithm = signatureProvider.signer().getHashAlgorithm();
//        
//        HashProvider hashProvider = session.getProvider(HashProvider.class, hashAlgorithm);
//        byte[] hash = hashProvider.hash(input);
//
//        return HashUtils.encodeHashToOIDC(hash);
//    }
	
	
}
