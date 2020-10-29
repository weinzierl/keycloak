package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = As.PROPERTY, property = "classtype")
@JsonSubTypes({
	@JsonSubTypes.Type(value = OPMetadata.class, name = "OPMetadata")
})
public interface MetadataI {
	
	
}
