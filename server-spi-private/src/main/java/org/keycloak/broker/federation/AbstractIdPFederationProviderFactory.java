package org.keycloak.broker.federation;

import org.keycloak.models.KeycloakSession;

public abstract class AbstractIdPFederationProviderFactory <T extends AbstractIdPFederationProvider> implements IdpFederationProviderFactory {
	
    @Override
    public T create(KeycloakSession session) {
        return null;
    }
	
    
}
