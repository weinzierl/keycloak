package org.keycloak.protocol.oidc.federation.paths;

import java.util.ArrayList;
import java.util.List;

import org.keycloak.protocol.oidc.federation.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.beans.OIDCFederationClientRepresentationPolicy;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

//@JsonIgnoreProperties(value = { "parsedChain", "combinedPolicies" })
public class TrustChain {

    private List<String> chain;
    private List<EntityStatement> parsedChain;
    private OIDCFederationClientRepresentationPolicy combinedPolicy;
    
    public TrustChain() {
        chain = new ArrayList<String>();
        parsedChain = new ArrayList<EntityStatement>();
        combinedPolicy = new OIDCFederationClientRepresentationPolicy();
    }
    
    public List<String> getChain() {
        return chain;
    }
    public void setChain(List<String> chain) {
        this.chain = chain;
    }
    
    public List<EntityStatement> getParsedChain() {
        return parsedChain;
    }
    public void setParsedChain(List<EntityStatement> parsedChain) {
        this.parsedChain = parsedChain;
    }

    public OIDCFederationClientRepresentationPolicy getCombinedPolicy() {
        return combinedPolicy;
    }
    public void setCombinedPolicy(OIDCFederationClientRepresentationPolicy combinedPolicy) {
        this.combinedPolicy = combinedPolicy;
    }
    
}
