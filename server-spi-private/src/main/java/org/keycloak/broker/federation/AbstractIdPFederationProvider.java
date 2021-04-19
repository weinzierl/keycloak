package org.keycloak.broker.federation;

import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;

public abstract class AbstractIdPFederationProvider <T extends FederationModel> implements FederationProvider {
	
    protected final KeycloakSession session;
    protected final T model;
    protected final String realmId;

    public AbstractIdPFederationProvider(KeycloakSession session, T model,String realmId) {
        this.session = session;
        this.model = model;
        this.realmId = realmId;
    }
    
	
}