package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class Metadata {
    
    @JsonProperty("openid_provider")
    private OIDCFederationConfigurationRepresentation op;
    
    @JsonProperty("federation_entity")
    private FederationEntity federationEntity;
    
    @JsonProperty("openid_relying_party")
    private OIDCFederationClientRepresentation rp;

    public OIDCFederationConfigurationRepresentation getOp() {
        return op;
    }

    public void setOp(OIDCFederationConfigurationRepresentation op) {
        this.op = op;
    }

    public FederationEntity getFederationEntity() {
        return federationEntity;
    }

    public void setFederationEntity(FederationEntity federationEntity) {
        this.federationEntity = federationEntity;
    }

    public OIDCFederationClientRepresentation getRp() {
        return rp;
    }

    public void setRp(OIDCFederationClientRepresentation rp) {
        this.rp = rp;
    }

    
}
