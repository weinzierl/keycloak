package org.keycloak.protocol.oidc.federation.processes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.federation.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.exceptions.BadSigningOrEncryptionException;
import org.keycloak.protocol.oidc.federation.exceptions.UnparsableException;
import org.keycloak.protocol.oidc.federation.helpers.Remote;
import org.keycloak.protocol.oidc.federation.paths.TrustChainParsed;
import org.keycloak.protocol.oidc.federation.paths.TrustChainRaw;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JOSEObjectType;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.DefaultJOSEObjectTypeVerifier;
import com.nimbusds.jose.proc.JWSKeySelector;
import com.nimbusds.jose.proc.JWSVerificationKeySelector;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.ConfigurableJWTProcessor;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;

public class TrustChainProcessor {

	private static ObjectMapper om = new ObjectMapper();
	
	private KeycloakSession session;

	public TrustChainProcessor(KeycloakSession session) {
		this.session = session;
	}

	
	/**
	 * @throws BadSigningOrEncryptionException 
	 * @throws UnparsableException 
	 * This should construct all possible trust chains from a given leaf node url to a trust anchor url 
	 * @param leafNodeBaseUrl  this url should point to the base path of the leaf node (without the .well-known discovery subpath)
	 * @param trustAnchorId this should hold the trust anchor id
	 * @return any valid trust chains from the leaf node to the trust anchor.
	 * @throws IOException 
	 * @throws  
	 */
	public List<TrustChainRaw> constructTrustChains(String leafNodeBaseUrl, Set<String> trustAnchorIds) throws IOException, UnparsableException, BadSigningOrEncryptionException {		
		//TODO: create all possible links from the leaf node to the trust anchor
		
		trustAnchorIds.stream().map(s -> s.trim()).collect(Collectors.toCollection(HashSet::new));
		
		String encodedLeafES = Remote.getContentFrom(new URL(leafNodeBaseUrl + "/.well-known/openid-federation"));
		
		if(!validateLinkSig(encodedLeafES))
			return new ArrayList<TrustChainRaw>();
		
		TrustChainRaw trustChainRaw = new TrustChainRaw();
		trustChainRaw.add(encodedLeafES);
		fetchSubChain(trustChainRaw, trustAnchorIds);
		
		
		//first find all possible paths
		
		return new ArrayList<TrustChainRaw>();
	}
	
	private void fetchSubChain(TrustChainRaw trustChainRaw, Set<String> trustAnchorIds) {

		String encodedES = trustChainRaw.get(trustChainRaw.size()-1);
		EntityStatement es;
		try {
			es = parseChainLink(encodedES);
		} catch (UnparsableException e) {
			System.out.println("Cannot process a subchain link. Might not be able to form a trustchain. " + e.getMessage());
			return;
		}
		if(es.getAuthorityHints() == null || es.getAuthorityHints().isEmpty()) {
			if(es.getAuthorityHints().stream().anyMatch(authHint -> trustAnchorIds.contains(authHint)))
				materializeChain(trustChainRaw);
		}
		else {
			es.getAuthorityHints().forEach(authHint -> {
				try {
					String encodedSubES = Remote.getContentFrom(new URL(authHint + "/.well-known/openid-federation"));
					if(validateLinkSig(encodedES)) {
						TrustChainRaw subChainRaw = new TrustChainRaw();
						subChainRaw.addAll(trustChainRaw);
						subChainRaw.add(encodedSubES);
						fetchSubChain(subChainRaw, trustAnchorIds);
					}
				}
				catch(Exception ex) {
					System.out.println("Cannot process a subchain. Might not be able to form a trustchain. " + ex.getMessage());
				}
			});
		}
	}
	
	private void materializeChain(TrustChainRaw trustChainRaw) {
		
	}
	
	/**
	 * This validates the whole trustChain signature
	 * @param trustChainRaw
	 * @return
	 * @throws IOException
	 * @throws UnparsableException
	 * @throws BadSigningOrEncryptionException
	 */
	public boolean validateTrustChain(TrustChainRaw trustChainRaw) throws IOException, UnparsableException, BadSigningOrEncryptionException {
		//TODO: first validate the integrity of the contained information, as described at chapter 7.2 of OpenID Connect Federation 1.0
		
		//below, we validate the signatures
		for(String chainLink : trustChainRaw) {
			if(!validateLinkSig(chainLink))
				return false;
		}
		return true;
	}
	
	public TrustChainParsed transformTrustChain(TrustChainRaw trustChainRaw) throws UnparsableException {
		TrustChainParsed tcp = new TrustChainParsed();
		for(String tcr : trustChainRaw) 
			tcp.add(parseChainLink(tcr));
		return tcp;
	}

	
	
	public static boolean validateLinkSig(String token) throws IOException, UnparsableException, BadSigningOrEncryptionException {
		String jsonKey = om.writeValueAsString(parseChainLink(token).getJwks());
		return validateLinkSig(jsonKey, token);
	}
	
	public static boolean validateLinkSig(String jsonKey, String token) throws IOException, UnparsableException, BadSigningOrEncryptionException {
		try{
			JWKSet jwkSet = JWKSet.load(new ByteArrayInputStream(jsonKey.getBytes()));
			JWKSource<SecurityContext> keySource = new ImmutableJWKSet<SecurityContext>(jwkSet);
			
			ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
			jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("JWT")));
			JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
			jwtProcessor.setJWSKeySelector(keySelector);
	
			SecurityContext ctx = null; // optional context parameter
			JWTClaimsSet claimsSet = jwtProcessor.process(token, ctx);
	
	//		System.out.println(claimsSet.toJSONObject());
		}
		catch(ParseException ex) {
			throw new UnparsableException(ex.getMessage());
		}
		catch(BadJOSEException | JOSEException ex) {
			throw new BadSigningOrEncryptionException(ex.getMessage());
		}
		return true;
		
	}
	
	
	public static EntityStatement parseChainLink(String endodedChainLink) throws UnparsableException {
		String [] splits = endodedChainLink.split("\\.");
		if(splits.length != 3)
			throw new UnparsableException("Trust chain contains a chain-link which does not abide to the dot-delimited format of xxx.yyy.zzz");
		try {
			return om.readValue(Base64.getDecoder().decode(splits[1]), EntityStatement.class);
		} catch (JsonParseException e) {
			throw new UnparsableException("Trust chain link contains an entity statement which is not json-encoded");
		} catch (JsonMappingException e) {
			throw new UnparsableException("Trust chain link contains an entity statement which can not be mapped to EntityStatement.class");
		} catch (IOException e) {
			throw new UnparsableException(e.getMessage());
		}
	}

	
}