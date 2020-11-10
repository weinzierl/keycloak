package org.keycloak.protocol.oidc.federation.processes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.federation.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.exceptions.BadSigningOrEncryptionException;
import org.keycloak.protocol.oidc.federation.exceptions.UnparsableException;
import org.keycloak.protocol.oidc.federation.helpers.FedUtils;
import org.keycloak.protocol.oidc.federation.paths.TrustChainParsed;
import org.keycloak.protocol.oidc.federation.paths.TrustChainRaw;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
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
	 * @param trustAnchorId this should hold the trust anchor ids
	 * @return any valid trust chains from the leaf node to the trust anchor.
	 * @throws IOException 
	 * @throws  
	 */
	public List<TrustChainRaw> constructTrustChains(String leafNodeBaseUrl, Set<String> trustAnchorIds) throws IOException, UnparsableException, BadSigningOrEncryptionException {		
		trustAnchorIds.stream().map(s -> s.trim()).collect(Collectors.toCollection(HashSet::new));
		String encodedLeafES = FedUtils.getContentFrom(new URL(leafNodeBaseUrl + "/.well-known/openid-federation"));
		
		printIssSub(encodedLeafES);
		
		List<TrustChainRaw> trustChains = subTrustChains(encodedLeafES, trustAnchorIds);
		trustChains.forEach(trustChain -> trustChain.add(0, encodedLeafES)); //add also the leaf node 
		return trustChains;
		
	}
	
	private List<TrustChainRaw> subTrustChains(String encodedNode, Set<String> trustAnchorIds) {

		List<TrustChainRaw> chainsList = new ArrayList<>();
		
		EntityStatement es;
		try {
			es = parseAndValidateChainLink(encodedNode);
		} catch (UnparsableException | BadSigningOrEncryptionException e ) {
			System.out.println("Cannot process a subchain link. Might not be able to form a trustchain. " + e.getMessage());
			return chainsList;
		}
		if(es.getAuthorityHints() == null || es.getAuthorityHints().isEmpty()) {
			if(trustAnchorIds.contains(es.getIssuer())) {
				TrustChainRaw trustChainRaw = new TrustChainRaw();
//				trustChainRaw.add(encodedNode); //this is the self-issued statement of a trust anchor. Should not add it in the chain (as of oidc fed spec version draft 12) 
				chainsList.add(trustChainRaw);
			}
		}
		else {
			
			es.getAuthorityHints().forEach(authHint -> {
				try {
					String encodedSubNodeSelf = FedUtils.getContentFrom(new URL(authHint + "/.well-known/openid-federation"));
					EntityStatement subNodeSelfES = parseAndValidateChainLink(encodedSubNodeSelf);
//					printIssSub(encodedSubNodeSelf);
					String fedApiUrl = subNodeSelfES.getMetadata().getFederationEntity().getFederationApiEndpoint();
					String encodedSubNodeSubordinate = FedUtils.getContentFrom(new URL(fedApiUrl + "?iss="+urlEncode(subNodeSelfES.getIssuer())+"&sub="+urlEncode(es.getIssuer())));					
					EntityStatement subNodeSubordinateES = parseAndValidateChainLink(encodedSubNodeSubordinate);
//					printIssSub(encodedSubNodeSubordinate);
					//TODO: might want to make some more checks on subNodeSubordinateES integrity
					List<TrustChainRaw> subList = subTrustChains(encodedSubNodeSelf, trustAnchorIds);
					for(TrustChainRaw tcr : subList) {
						tcr.add(0, encodedSubNodeSubordinate);
						chainsList.add(tcr);
					}
				}
				catch(Exception ex) {
					ex.printStackTrace();
				}
				
			});
			
		}
		
		return chainsList;
		
	}
	
	private String urlEncode(String url) throws UnsupportedEncodingException {
		return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
	}
	
	private String urlDecode(String url) throws UnsupportedEncodingException {
		return URLDecoder.decode(url, StandardCharsets.UTF_8.toString());
	}
	
	private void printIssSub(String token) throws UnparsableException {
		EntityStatement statement = parseChainLink(token);
		System.out.println(String.format("Asked (+ validated) %s about %s. AuthHints: %s", statement.getIssuer(), statement.getSubject(), statement.getAuthorityHints()));
	}
	
	/**
	 * This validates the whole trustChain signature
	 * @param trustChainRaw
	 * @return
	 * @throws IOException
	 * @throws UnparsableException
	 * @throws BadSigningOrEncryptionException
	 */
	public boolean validateTrustChain(TrustChainRaw trustChainRaw) {
		for(String chainLink : trustChainRaw) {
			try {
				EntityStatement es = parseAndValidateChainLink(chainLink);
				//TODO: might also want to validate the integrity of the contained information, as described at chapter 7.2 of OpenID Connect Federation 1.0
			}
			catch( UnparsableException | BadSigningOrEncryptionException ex) {
				ex.printStackTrace();
				return false;
			}
		}
		return true;
	}
	
	public TrustChainParsed transformTrustChain(TrustChainRaw trustChainRaw) throws UnparsableException {
		TrustChainParsed tcp = new TrustChainParsed();
		for(String tcr : trustChainRaw) 
			tcp.add(parseChainLink(tcr));
		return tcp;
	}

	
	
	public static EntityStatement parseAndValidateChainLink(String token) throws UnparsableException, BadSigningOrEncryptionException {
	    EntityStatement statement = parseChainLink(token);
	    try{
	        String jsonKey = om.writeValueAsString(statement.getJwks());
	        
			JWKSet jwkSet = JWKSet.load(new ByteArrayInputStream(jsonKey.getBytes()));
			JWKSource<SecurityContext> keySource = new ImmutableJWKSet<SecurityContext>(jwkSet);
			
			ConfigurableJWTProcessor<SecurityContext> jwtProcessor = new DefaultJWTProcessor<>();
			//jwtProcessor.setJWSTypeVerifier(new DefaultJOSEObjectTypeVerifier<>(new JOSEObjectType("JWT")));
			JWSKeySelector<SecurityContext> keySelector = new JWSVerificationKeySelector<>(JWSAlgorithm.RS256, keySource);
			jwtProcessor.setJWSKeySelector(keySelector);
	
			SecurityContext ctx = null; // optional context parameter
			JWTClaimsSet claimsSet = jwtProcessor.process(token, ctx);
			
		}
		catch(IOException ex) {
			throw new UnparsableException(ex.getMessage());
		}
		catch(ParseException ex) {
			throw new UnparsableException(ex.getMessage());
		}
		catch(BadJOSEException | JOSEException ex) {
			throw new BadSigningOrEncryptionException(ex.getMessage());
		}
	    
		return statement;
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
		    e.printStackTrace();
			throw new UnparsableException("Trust chain link contains an entity statement which can not be mapped to EntityStatement.class");
		} catch (IOException e) {
			throw new UnparsableException(e.getMessage());
		}
	}

	
}
