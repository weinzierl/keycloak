package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataPolicy {

    @JsonProperty("openid_relying_party")
    private OIDCFederationClientRepresentationPolicy rpPolicy;

    public MetadataPolicy(OIDCFederationClientRepresentationPolicy rpPolicy) {
        this.rpPolicy =rpPolicy;
    }

    public OIDCFederationClientRepresentationPolicy getRpPolicy() {
        return rpPolicy;
    }

    public void setRpPolicy(OIDCFederationClientRepresentationPolicy rpPolicy) {
        this.rpPolicy = rpPolicy;
    }
    
}
