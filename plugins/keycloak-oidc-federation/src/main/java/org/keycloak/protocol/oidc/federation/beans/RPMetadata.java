package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class RPMetadata implements MetadataI {
    
    @JsonProperty("openid_relying_party")
    private OIDCFederationClientRepresentation relyingParty;

    public OIDCFederationClientRepresentation getRelyingParty() {
        return relyingParty;
    }

    public void setRelyingParty(OIDCFederationClientRepresentation relyingParty) {
        this.relyingParty = relyingParty;
    }
    
}
