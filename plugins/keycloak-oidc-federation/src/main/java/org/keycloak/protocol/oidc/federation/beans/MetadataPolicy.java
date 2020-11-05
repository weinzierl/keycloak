package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetadataPolicy {

    @JsonProperty("openid_relying_party")
    private OIDCFederationClientRepresentationPolicy rpPolicy;
    
    @JsonProperty("openid_provider")
    private OIDCFederationConfigurationRepresentationPolicy opPolicy;

    public MetadataPolicy(OIDCFederationClientRepresentationPolicy rpPolicy) {
        this.rpPolicy =rpPolicy;
    }
    
    public MetadataPolicy(OIDCFederationConfigurationRepresentationPolicy opPolicy) {
        this.opPolicy =opPolicy;
    }

    public OIDCFederationClientRepresentationPolicy getRpPolicy() {
        return rpPolicy;
    }

    public void setRpPolicy(OIDCFederationClientRepresentationPolicy rpPolicy) {
        this.rpPolicy = rpPolicy;
    }
    
}
