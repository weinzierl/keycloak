package org.keycloak.protocol.oidc.federation.model;

import javax.persistence.EntityManager;
import javax.ws.rs.NotFoundException;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class ConfigurationService {
    
    private final KeycloakSession session;

    private final EntityManager em;

    public ConfigurationService(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    private RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    public ConfigurationEntity getEntity() {
        return em.find(ConfigurationEntity.class, getRealm().getId());
    }
    
    public void saveEntity(ConfigurationEntity config) {
        em.persist(config);
    }
    
    
    public void deleteEntity() throws NotFoundException{
        ConfigurationEntity entity = em.find(ConfigurationEntity.class, getRealm().getId());
        if (entity == null) 
            throw new NotFoundException(String.format("Realm %s does not have", getRealm().getName()));
        em.remove(entity);
    }

}
