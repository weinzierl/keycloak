package org.keycloak.protocol.oidc.federation.model;

import org.keycloak.Config.Scope;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProvider;
import org.keycloak.connections.jpa.entityprovider.JpaEntityProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class AuthorityHintJpaEntityProviderFactory implements JpaEntityProviderFactory {

    protected static final String ID = "authority-hint-entity-provider";
    
    @Override
    public JpaEntityProvider create(KeycloakSession session) {
        return new AuthorityHintJpaEntityProvider();
    }

    @Override
    public void init(Scope config) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // TODO Auto-generated method stub
        
    }

    @Override
    public void close() {
        // TODO Auto-generated method stub
        
    }

    @Override
    public String getId() {
       return ID;
    }

}
