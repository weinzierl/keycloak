package org.keycloak.models.cache.infinispan;

import java.util.Set;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.IdentityProviderCacheInvalidationEvent;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;
import org.keycloak.models.cache.infinispan.events.RealmCacheInvalidationEvent;
import org.keycloak.models.cache.infinispan.stream.HasRolePredicate;
import org.keycloak.models.cache.infinispan.stream.InClientPredicate;
import org.keycloak.models.cache.infinispan.stream.InRealmPredicate;

public class IdpCacheManager extends CacheManager {

	private static final Logger logger = Logger.getLogger(IdpCacheManager.class);
	
	public IdpCacheManager(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
		super(cache, revisions);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}
	
	
	public void identityProviderUpdated(String id, String alias, String realmId, Set<String> invalidations) {
        invalidations.add(IdpCacheSession.getIdentityProviderByIdCacheKey(id, realmId));
        invalidations.add(IdpCacheSession.getIdentityProviderByAliasCacheKey(alias, realmId));
    }

    public void identityProviderRemoved(String id, String alias, String realmId, Set<String> invalidations) {
    	invalidations.add(IdpCacheSession.getIdentityProviderByIdCacheKey(id, realmId));
        invalidations.add(IdpCacheSession.getIdentityProviderByAliasCacheKey(alias, realmId));
    }
	
    
    public void identityProviderMapperAdded(String realmId, String mapperUUID, String idpMapperName, Set<String> invalidations) {
//        invalidations.add(RealmCacheSession.getRealmClientsQueryCacheKey(realmId));
    }

    public void identityProviderMapperUpdated(String realmId, String mapperUUID, String idpMapperName, Set<String> invalidations) {
//        invalidations.add(RealmCacheSession.getClientByClientIdCacheKey(clientId, realmId));
    }


    public void identityProviderMapperRemoved(String realmId, String mapperUUID, String idpMapperName, Set<String> invalidations) {
//        invalidations.add(RealmCacheSession.getRealmClientsQueryCacheKey(realmId));
//        invalidations.add(RealmCacheSession.getClientByClientIdCacheKey(clientId, realmId));
//
//        addInvalidations(InClientPredicate.create().client(clientUUID), invalidations);
    }
    
	
    @Override
    protected void addInvalidationsFromEvent(InvalidationEvent event, Set<String> invalidations) {
        invalidations.add(event.getId());
        ((IdentityProviderCacheInvalidationEvent) event).addInvalidations(this, invalidations);
    }
	
    
    public void invalidateRealmIdentityProviders(String realm, Set<String> invalidations) {
        InRealmPredicate inRealmPredicate = getInRealmPredicate(realm);
        addInvalidations(inRealmPredicate, invalidations);
    }

    private InRealmPredicate getInRealmPredicate(String realmId) {
        return InRealmPredicate.create().realm(realmId);
    }
	
}
