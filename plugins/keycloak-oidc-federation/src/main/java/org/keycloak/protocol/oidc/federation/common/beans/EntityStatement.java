package org.keycloak.protocol.oidc.federation.common.beans;

import java.util.List;

import org.keycloak.TokenCategory;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.representations.JsonWebToken;

import com.fasterxml.jackson.annotation.JsonProperty;

public class EntityStatement extends JsonWebToken {

    @JsonProperty("authority_hints")
    protected List<String> authorityHints;
	
    protected JSONWebKeySet jwks;
    
    protected Metadata metadata;
    
    @JsonProperty("metadata_policy")
    protected MetadataPolicy metadataPolicy;
    
    
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

	public Metadata getMetadata() {
		return metadata;
	}

	public void setMetadata(Metadata metadata) {
		this.metadata = metadata;
	}
    
    public MetadataPolicy getMetadataPolicy() {
        return metadataPolicy;
    }

    public void setMetadataPolicy(MetadataPolicy metadataPolicy) {
        this.metadataPolicy = metadataPolicy;
    }

    @Override
    public TokenCategory getCategory() {
        return TokenCategory.ACCESS; //treat it as an access token (use asymmetric crypto algorithms)
    }
    
	
}
