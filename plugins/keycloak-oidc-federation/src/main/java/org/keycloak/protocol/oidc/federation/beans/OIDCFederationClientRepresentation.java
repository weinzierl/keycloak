package org.keycloak.protocol.oidc.federation.beans;

import java.util.List;

import org.keycloak.representations.oidc.OIDCClientRepresentation;

public class OIDCFederationClientRepresentation extends OIDCClientRepresentation {
    
    private List<ClientRegistrationEnum> client_registration_types;

    private String organization_name;

    private String trust_anchor_id;
    
    public List<ClientRegistrationEnum> getClient_registration_types() {
        return client_registration_types;
    }

    public void setClient_registration_types(List<ClientRegistrationEnum> client_registration_types) {
        this.client_registration_types = client_registration_types;
    }

    public String getOrganization_name() {
        return organization_name;
    }

    public void setOrganization_name(String organization_name) {
        this.organization_name = organization_name;
    }

    public String getTrust_anchor_id() {
        return trust_anchor_id;
    }

    public void setTrust_anchor_id(String trust_anchor_id) {
        this.trust_anchor_id = trust_anchor_id;
    }
    
}
