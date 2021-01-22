package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.CacheIdpProviderFactoryI;
import org.keycloak.models.cache.CacheIdpProviderI;
import org.keycloak.models.cache.infinispan.entities.CachedIdentityProviderMappers;
import org.keycloak.models.cache.infinispan.entities.CachedIdentityProviders;
import org.keycloak.models.cache.infinispan.entities.Revisioned;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

public class InfinispanCacheIdpProviderFactory implements CacheIdpProviderFactoryI {

    private static final Logger log = Logger.getLogger(InfinispanCacheIdpProviderFactory.class);
    
    public static final String IDP_CLEAR_CACHE_EVENTS = "IDP_CLEAR_CACHE_EVENTS";
    public static final String IDP_INVALIDATION_EVENTS = "IDP_INVALIDATION_EVENTS";
	
    protected Cache<String, CachedIdentityProviders> idpCache;
    protected Cache<String, CachedIdentityProviderMappers> idpMappersCache;
    
	protected volatile IdpCacheProvider idpCacheProvider;
	
	
	@Override
	public CacheIdpProviderI create(KeycloakSession session) {
		lazyInit(session);
		idpCacheProvider = new IdpCacheProvider(idpCache, idpMappersCache, session);
		return idpCacheProvider;
	}

	private void lazyInit(KeycloakSession session) {
        if (idpCache == null) {
            synchronized (this) {
                if (idpCache == null) {
                	idpCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.IDP_CACHE_NAME);
                }
            }
        }
        if (idpMappersCache == null) {
            synchronized (this) {
                if (idpMappersCache == null) {
                    idpMappersCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.IDP_MAPPERS_CACHE_NAME);
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
