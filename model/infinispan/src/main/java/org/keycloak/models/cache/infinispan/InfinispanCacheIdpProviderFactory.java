package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.CacheIdpProvider;
import org.keycloak.models.cache.CacheIdpProviderFactory;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

public class InfinispanCacheIdpProviderFactory implements CacheIdpProviderFactory {

    private static final Logger log = Logger.getLogger(InfinispanCacheIdpProviderFactory.class);
    
    public static final String IDP_CLEAR_CACHE_EVENTS = "IDP_CLEAR_CACHE_EVENTS";
    public static final String IDP_INVALIDATION_EVENTS = "IDP_INVALIDATION_EVENTS";
	
	protected volatile IdpCacheManager idpCache;
	
	
	@Override
	public CacheIdpProvider create(KeycloakSession session) {
		lazyInit(session);
        return new IdpCacheSession(idpCache, session);
	}

	private void lazyInit(KeycloakSession session) {
        if (idpCache == null) {
            synchronized (this) {
                if (idpCache == null) {
                    Cache<String, Revisioned> cache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.IDP_CACHE_NAME);
                    Cache<String, Long> revisions = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.IDP_REVISIONS_CACHE_NAME);
                    idpCache = new IdpCacheManager(cache, revisions);

                    ClusterProvider cluster = session.getProvider(ClusterProvider.class);

                    cluster.registerListener(IDP_INVALIDATION_EVENTS, (ClusterEvent event) -> {

                        InvalidationEvent invalidationEvent = (InvalidationEvent) event;
                        idpCache.invalidationEventReceived(invalidationEvent);

                    });

                    cluster.registerListener(IDP_CLEAR_CACHE_EVENTS, (ClusterEvent event) -> {

                        idpCache.clear();

                    });

                    log.debug("Registered cluster listeners for IdentityProviders Cache");
                }
            }
        }
    }
	
	@Override
	public void init(Scope config) {
		
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		
	}

	@Override
	public void close() {
		
	}

	@Override
	public String getId() {
		return "default";
	}

}
