package org.keycloak.models.cache.infinispan;

import java.util.Set;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

public class IdpCacheManager extends CacheManager {

	private static final Logger logger = Logger.getLogger(IdpCacheManager.class);
	
	public IdpCacheManager(Cache<String, Revisioned> cache, Cache<String, Long> revisions) {
		super(cache, revisions);
	}

	@Override
	protected Logger getLogger() {
		return logger;
	}

	@Override
	protected void addInvalidationsFromEvent(InvalidationEvent event, Set<String> invalidations) {
		
	}
	

}
