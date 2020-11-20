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

import org.jboss.logging.Logger;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.federation.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.beans.MetadataPolicy;
import org.keycloak.protocol.oidc.federation.beans.OIDCFederationClientRepresentationPolicy;
import org.keycloak.protocol.oidc.federation.exceptions.BadSigningOrEncryptionException;
import org.keycloak.protocol.oidc.federation.exceptions.InvalidTrustChainException;
import org.keycloak.protocol.oidc.federation.exceptions.MetadataPolicyCombinationException;
import org.keycloak.protocol.oidc.federation.exceptions.RemoteFetchingException;
import org.keycloak.protocol.oidc.federation.exceptions.UnparsableException;
import org.keycloak.protocol.oidc.federation.helpers.FedUtils;
import org.keycloak.protocol.oidc.federation.helpers.MetadataPolicyUtils;
import org.keycloak.protocol.oidc.federation.paths.TrustChain;
import org.keycloak.protocol.oidc.federation.rest.op.FederationOPService;

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

    private static final Logger logger = Logger.getLogger(TrustChainProcessor.class);
    
	private static ObjectMapper om = new ObjectMapper();
	
	private KeycloakSession session;

	public TrustChainProcessor(KeycloakSession session) {
		this.session = session;
	}

	
	
	
	/**
     * This should construct all possible trust chains from a given leaf node url to a set of trust anchor urls 
     * @param leafNodeBaseUrl  this url should point to the base path of the leaf node (without the .well-known discovery subpath)
     * @param trustAnchorId this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node to the trust anchor.
     * @throws IOException 
     */
    public List<TrustChain> constructTrustChainsFromUrl(String leafNodeBaseUrl, Set<String> trustAnchorIds) throws IOException {      
        String encodedLeafES = FedUtils.getContentFrom(new URL(leafNodeBaseUrl + "/.well-known/openid-federation"));
        return constructTrustChainsFromJWT(encodedLeafES, trustAnchorIds);
    }
	
	
    /**
     * This should construct all possible trust chains from a given leaf node self-signed and encoded JWT to a set of trust anchor urls 
     * @param leafJWT  this is the self-signed JWT EntityStatement of a leaf node (Relay party or Openid Provider)
     * @param trustAnchorId this should hold the trust anchor ids
     * @return any valid trust chains from the leaf node JWT to the trust anchor.
     */
	public List<TrustChain> constructTrustChainsFromJWT(String leafJWT, Set<String> trustAnchorIds) {
	    
	    trustAnchorIds = trustAnchorIds.stream().map(s -> s.trim()).collect(Collectors.toCollection(HashSet::new));
	    
	    try {
	        EntityStatement leafEs = parse(leafJWT);
	        logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", leafEs.getIssuer(), leafEs.getSubject(), leafEs.getAuthorityHints()));
	    }catch(UnparsableException ex) {
	        logger.debug("Leaf's self-signed entity statement is not a valid jwt. Will return no chains...");
	    }
	    
        List<TrustChain> trustChains = subTrustChains(leafJWT, trustAnchorIds);
        
        //add also the leaf self-signed node
        trustChains.forEach(trustChain -> trustChain.getChain().add(0, leafJWT));
        
        
        trustChains.stream().map(trustChain -> {
            //parse chain nodes
            List<EntityStatement> parsedChain = trustChain.getChain().stream().map(node -> { 
                    try{
                        return parse(node); 
                    }
                    catch(UnparsableException ex) {
                        logger.debug("Cannot parse a node of a chain. Ignoring the whole chain as invalid");
                        return null;
                    }
                })
                .filter(node -> node!=null)
                .collect(Collectors.toList());
            if(parsedChain.size() == trustChain.getChain().size())
                trustChain.setParsedChain(parsedChain);
            else
                trustChain = null;

            //combine policies if valid till now
            if(trustChain != null && parsedChain.size()>1) {
                MetadataPolicy metadataPolicy = parsedChain.get(parsedChain.size()-1).getMetadataPolicy();
                OIDCFederationClientRepresentationPolicy combinedPolicy = parsedChain.get(parsedChain.size()-1).getMetadataPolicy().getRpPolicy();
                for(int i=parsedChain.size()-2; i>0; i--) {
                    try {
                        combinedPolicy = MetadataPolicyUtils.combineClientPOlicies(combinedPolicy, parsedChain.get(i).getMetadataPolicy().getRpPolicy());
                    }
                    catch (MetadataPolicyCombinationException e) {
                        logger.debug(String.format("Cannot combine metadata policy of iss=%s sub=%s and its inferiors", parsedChain.get(i).getIssuer(), parsedChain.get(i).getSubject()));
                        combinedPolicy = null;
                    }
                }
                if(combinedPolicy!=null)
                    trustChain.setCombinedPolicy(combinedPolicy);
                else
                    trustChain = null;
            }
            
            return trustChain;
        })
        .filter(chain -> chain!=null)
        .collect(Collectors.toList());

        //fill-in the trustAnchorId and leafId values
        trustChains.forEach(trustChain -> {
            //trust chain always has at least one element!
            trustChain.setTrustAnchorId(trustChain.getParsedChain().get(trustChain.getParsedChain().size()-1).getIssuer());
            trustChain.setLeafId(trustChain.getParsedChain().get(0).getIssuer());
        });
        
        
        return trustChains;
	
	}
	
	
	private List<TrustChain> subTrustChains(String encodedNode, Set<String> trustAnchorIds) {

		List<TrustChain> chainsList = new ArrayList<>();
		
		EntityStatement es;
		try {
			es = parseAndValidateSelfSigned(encodedNode);
		} catch (UnparsableException | BadSigningOrEncryptionException e ) {
			logger.info("Cannot process a subchain link. Might not be able to form a trustchain. " + e.getMessage());
			return chainsList;
		}
		if(es.getAuthorityHints() == null || es.getAuthorityHints().isEmpty()) {
			if(trustAnchorIds.contains(es.getIssuer())) {
				TrustChain trustChain = new TrustChain();
//				trustChainRaw.add(encodedNode); //this is the self-issued statement of a trust anchor. Should not add it in the chain (as of oidc fed spec version draft 12) 
				chainsList.add(trustChain);
			}
		}
		else {
			
			es.getAuthorityHints().forEach(authHint -> {
				try {
					String encodedSubNodeSelf = FedUtils.getContentFrom(new URL(authHint + "/.well-known/openid-federation"));
					EntityStatement subNodeSelfES = parseAndValidateSelfSigned(encodedSubNodeSelf);
					logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSelfES.getIssuer(), subNodeSelfES.getSubject(), subNodeSelfES.getAuthorityHints()));
					String fedApiUrl = subNodeSelfES.getMetadata().getFederationEntity().getFederationApiEndpoint();
					String encodedSubNodeSubordinate = FedUtils.getContentFrom(new URL(fedApiUrl + "?iss="+urlEncode(subNodeSelfES.getIssuer())+"&sub="+urlEncode(es.getIssuer())));					
					EntityStatement subNodeSubordinateES = parse(encodedSubNodeSubordinate);
					validate(encodedSubNodeSubordinate, subNodeSelfES.getJwks());
					logger.debug(String.format("EntityStatement of %s about %s. AuthHints: %s", subNodeSubordinateES.getIssuer(), subNodeSubordinateES.getSubject(), subNodeSubordinateES.getAuthorityHints()));
					//TODO: might want to make some more checks on subNodeSubordinateES integrity
					List<TrustChain> subList = subTrustChains(encodedSubNodeSelf, trustAnchorIds);
					for(TrustChain tcr : subList) {
						tcr.getChain().add(0, encodedSubNodeSubordinate);
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
	
	
	/**
	 * This validates the whole trustChain signature
	 * @param trustChainRaw
	 * @return the outcome of the validation (true/false)
	 * @throws InvalidTrustChainException 
	 * @throws UnparsableException 
	 * @throws RemoteFetchingException 
	 * @throws BadSigningOrEncryptionException 
	 */
	public void validateTrustChain(TrustChain trustChain) throws InvalidTrustChainException, UnparsableException, RemoteFetchingException, BadSigningOrEncryptionException {
	    List<String> trustChainRaw = trustChain.getChain();
	    
	    if(trustChainRaw.size() < 2)
	        throw new InvalidTrustChainException("Trying to validate a trust chain with zero or one element. A trust chain should contain at least 2 elements.");
	    
	    String trustAnchorUri = parse(trustChainRaw.get(trustChainRaw.size()-1)).getIssuer();
        String trustAnchorSelfSigned;
        try {
            trustAnchorSelfSigned = FedUtils.getContentFrom(new URL(trustAnchorUri + "/.well-known/openid-federation"));
        } catch (IOException e) {
            throw new RemoteFetchingException(e.getMessage());
        }
        JSONWebKeySet trustAnchorKeys = parse(trustAnchorSelfSigned).getJwks();
	    
	    String trustAnchorSubordinate = trustChainRaw.get(trustChainRaw.size()-1);
        validate(trustAnchorSubordinate, trustAnchorKeys);

	    List<EntityStatement> parsedTrustChain = new ArrayList<EntityStatement>();
	    for(String entityRaw : trustChainRaw)
	        parsedTrustChain.add(parse(entityRaw));	            
	    
	    boolean correctlyLinked = true;
	    for(int i=parsedTrustChain.size()-1 ; i>0 ; i--) {
	        if(!parsedTrustChain.get(i).getSubject().trim().equals(parsedTrustChain.get(i-1).getIssuer().trim())) {
	            correctlyLinked = false;
	            break;
	        }
	    }
	    if(!correctlyLinked)
	        throw new InvalidTrustChainException("The trust chain should have entity[i].iss == entity[i+1].sub");
	    
	    boolean allIntermediatesValid = true;
	    for(int i=trustChainRaw.size()-2 ; i>0 ; i--)
            validate(trustChainRaw.get(i), parsedTrustChain.get(i+1).getJwks());
	    
	    if(!allIntermediatesValid)
	        throw new InvalidTrustChainException("The trust chain has one or more invalid signed ");
	    
	}

	
	public static void validate(String token, JSONWebKeySet publicKey) throws UnparsableException, BadSigningOrEncryptionException {
	    String jsonKey;
        try {
            jsonKey = om.writeValueAsString(publicKey);
        } catch (JsonProcessingException e) {
            throw new UnparsableException(e.getMessage());
        }
	    validate(token, jsonKey);
    }
	
	public static void validate(String token, String jsonPublicKey) throws UnparsableException, BadSigningOrEncryptionException {
        try{
            om.writeValueAsString(jsonPublicKey);
            JWKSet jwkSet = JWKSet.load(new ByteArrayInputStream(jsonPublicKey.getBytes()));
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
    }
	
	public static EntityStatement parseAndValidateSelfSigned(String token) throws UnparsableException, BadSigningOrEncryptionException {
	    EntityStatement statement = parse(token);
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
	
	
	
	public static EntityStatement parse(String token) throws UnparsableException {
		String [] splits = token.split("\\.");
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
