package org.keycloak.broker.federation;

import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractIdPFederationProvider <T extends IdentityProvidersFederationModel> implements IdpFederationProvider {
	
    protected final KeycloakSession session;
    protected final T model;
    protected final String realmId;

    public AbstractIdPFederationProvider(KeycloakSession session, T model,String realmId) {
        this.session = session;
        this.model = model;
        this.realmId = realmId;
    }
    
	
}