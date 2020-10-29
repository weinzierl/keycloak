package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FederationMetadata implements MetadataI {

    @JsonProperty("federation_entity")
    private FederationEntity federationEntity;

    public FederationEntity getFederationEntity() {
        return federationEntity;
    }

    public void setFederationEntity(FederationEntity federationEntity) {
        this.federationEntity = federationEntity;
    }

}
