package org.keycloak.protocol.oidc.federation.model;

import java.util.List;

import javax.persistence.EntityManager;
import javax.ws.rs.NotFoundException;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public class TrustAnchorService {
    
    private final KeycloakSession session;

    private final EntityManager em;

    public TrustAnchorService(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    public List<TrustAnchor> findTrustAnchorByRealm() {
        List<TrustAnchor> results = em.createNamedQuery("findTrustAnchorByRealm", TrustAnchor.class)
            .setParameter("realmId", getRealm().getId()).getResultList();

        return results;
    }
    
    public TrustAnchor create(String value) {
        TrustAnchor entity = new TrustAnchor();
        entity.setValue(value);
        entity.setRealmId(getRealm().getId());
        entity.setId(KeycloakModelUtils.generateId());
        em.persist(entity);
        return entity;
    }
    
    public void delete(String id) throws NotFoundException{
        TrustAnchor entity = em.find(TrustAnchor.class, id);
        if (entity == null) 
            throw new NotFoundException("TrustAnchor with id = "+ id  + " does not exists.");
        em.remove(entity);
    }

}
