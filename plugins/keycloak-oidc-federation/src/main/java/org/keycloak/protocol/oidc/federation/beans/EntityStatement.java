package org.keycloak.protocol.oidc.federation.beans;

import java.util.List;

import org.keycloak.TokenCategory;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityStatement extends JsonWebToken {

    @JsonProperty("authority_hints")
    protected List<String> authorityHints;
	
    @JsonProperty("jwks")
    protected JSONWebKeySet jwks;
    
    @JsonProperty("metadata")
    protected MetadataI metadata;

    
	public List<String> getAuthorityHints() {
		return authorityHints;
	}

	public void setAuthorityHints(List<String> authorityHints) {
		this.authorityHints = authorityHints;
	}

	public JSONWebKeySet getJwks() {
		return jwks;
	}

	public void setJwks(JSONWebKeySet jwks) {
		this.jwks = jwks;
	}

	public MetadataI getMetadata() {
		return metadata;
	}

	public void setMetadata(MetadataI metadata) {
		this.metadata = metadata;
	}
    
    @Override
    public TokenCategory getCategory() {
        return TokenCategory.ACCESS; //treat it as an access token (use asymmetric crypto algorithms)
    }
    
	
}
