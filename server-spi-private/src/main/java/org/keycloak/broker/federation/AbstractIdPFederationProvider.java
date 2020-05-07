package org.keycloak.broker.federation;

import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractIdPFederationProvider <T extends IdentityProvidersFederationModel> implements IdpFederationProvider {
	
    protected final KeycloakSession session;
    protected final T model;

    public AbstractIdPFederationProvider(KeycloakSession session, T model) {
        this.session = session;
        this.model = model;
    }
    
	
}