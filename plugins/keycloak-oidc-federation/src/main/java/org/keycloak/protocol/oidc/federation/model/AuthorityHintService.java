package org.keycloak.protocol.oidc.federation.model;

import java.util.List;

import javax.persistence.EntityManager;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.PathParam;

import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

public class AuthorityHintService {

    private final KeycloakSession session;

    private final EntityManager em;

    public AuthorityHintService(KeycloakSession session) {
        this.session = session;
        if (getRealm() == null) {
            throw new IllegalStateException("The service cannot accept a session without a realm in its context.");
        }
        em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
    }

    protected RealmModel getRealm() {
        return session.getContext().getRealm();
    }

    public List<AuthorityHint> findAuthorityHintsByRealm() {
        List<AuthorityHint> results = em.createNamedQuery("findAuthorityHintByRealm", AuthorityHint.class)
            .setParameter("realmId", getRealm().getId()).getResultList();

        return results;
    }
    
    public AuthorityHint create(String value) {
        AuthorityHint entity = new AuthorityHint();
        entity.setValue(value);
        entity.setRealmId(getRealm().getId());
        entity.setId(KeycloakModelUtils.generateId());
        em.persist(entity);
        return entity;
    }
    
    public void delete(String id) throws NotFoundException{
        AuthorityHint entity = em.find(AuthorityHint.class, id);
        if (entity == null) 
            throw new NotFoundException("AuthorityHint with id = "+ id  + " does not exists.");
        em.remove(entity);
    }


}
