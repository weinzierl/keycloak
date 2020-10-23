package org.keycloak.protocol.oidc.federation.beans;

import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OPMetadata implements MetadataI {

	@JsonProperty("openid_provider")
	private OIDCConfigurationRepresentation metadata;
	
	
	public OIDCConfigurationRepresentation getMetadata() {
		return metadata;
	}

	public void setMetadata(OIDCConfigurationRepresentation metadata) {
		this.metadata = metadata;
	}
	
}
