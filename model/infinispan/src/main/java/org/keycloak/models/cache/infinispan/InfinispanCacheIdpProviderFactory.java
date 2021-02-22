package org.keycloak.models.cache.infinispan;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.Config.Scope;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.connections.infinispan.InfinispanConnectionProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.CacheIdpProviderFactoryI;
import org.keycloak.models.cache.CacheIdpProviderI;
import org.keycloak.models.cache.infinispan.events.*;

import java.util.Set;

public class InfinispanCacheIdpProviderFactory implements CacheIdpProviderFactoryI {

    private static final Logger log = Logger.getLogger(InfinispanCacheIdpProviderFactory.class);
    
    public static final String IDP_CLEAR_CACHE_EVENT = "IDP_CLEAR_CACHE_EVENT";
	public static final String IDP_MAPPERS_CLEAR_CACHE_EVENT = "IDP_MAPPERS_CLEAR_CACHE_EVENT";

	
    protected Cache<String, Set<IdentityProviderModel>> idpCache;
    protected Cache<String, Set<IdentityProviderMapperModel>> idpMappersCache;
    
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
					ClusterProvider cluster = session.getProvider(ClusterProvider.class);

					cluster.registerListener(IdpAddedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpAddedEvent idpAddedEvent = (IdpAddedEvent) event;
						Set<IdentityProviderModel> idps = idpCache.get(idpAddedEvent.getRealmId());
						if(idps==null){
							log.info("Identity provider cache received a creation event before getting populated from the db. Ignoring...");
							return;
						}
						idps.add(idpAddedEvent.getIdentityProvider());
						idpCache.put(idpAddedEvent.getRealmId(), idps);
					});

					cluster.registerListener(IdpUpdatedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpUpdatedEvent idpUpdatedEvent = (IdpUpdatedEvent) event;
						Set<IdentityProviderModel> idps = idpCache.get(idpUpdatedEvent.getRealmId());
						if(idps==null){
							log.info("Identity provider cache received an update event before getting populated from the db. Ignoring...");
							return;
						}
						idps.remove(idpUpdatedEvent.getIdentityProvider());
						idps.add(idpUpdatedEvent.getIdentityProvider());
						idpCache.put(idpUpdatedEvent.getRealmId(), idps);
					});

					cluster.registerListener(IdpRemovedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpRemovedEvent idpRemovedEvent = (IdpRemovedEvent) event;
						Set<IdentityProviderModel> idps = idpCache.get(idpRemovedEvent.getRealmId());
						if(idps==null){
							log.info("Identity provider cache received a deletion event before getting populated from the db. Ignoring...");
							return;
						}
						idps.remove(idpRemovedEvent.getIdentityProvider());
						idpCache.put(idpRemovedEvent.getRealmId(), idps);
					});

					cluster.registerListener(InfinispanCacheIdpProviderFactory.IDP_CLEAR_CACHE_EVENT, (ClusterEvent event) -> {
						idpCache.clear();
					});
                }
            }
        }
        if (idpMappersCache == null) {
            synchronized (this) {
                if (idpMappersCache == null) {
                    idpMappersCache = session.getProvider(InfinispanConnectionProvider.class).getCache(InfinispanConnectionProvider.IDP_MAPPERS_CACHE_NAME);
					ClusterProvider cluster = session.getProvider(ClusterProvider.class);

					cluster.registerListener(IdpMapperAddedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpMapperAddedEvent idpMapperAddedEvent = (IdpMapperAddedEvent) event;
						Set<IdentityProviderMapperModel> idpMappers = idpMappersCache.get(idpMapperAddedEvent.getRealmId());
						if(idpMappers==null){
							log.info("Identity provider mapper cache received a creation event before getting populated from the db. Ignoring...");
							return;
						}
						idpMappers.add(idpMapperAddedEvent.getIdentityProviderMapper());
						idpMappersCache.put(idpMapperAddedEvent.getRealmId(), idpMappers);
					});

					cluster.registerListener(IdpMapperUpdatedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpMapperUpdatedEvent idpMapperUpdatedEvent = (IdpMapperUpdatedEvent) event;
						Set<IdentityProviderMapperModel> idpMappers = idpMappersCache.get(idpMapperUpdatedEvent.getRealmId());
						if(idpMappers==null){
							log.info("Identity provider mapper cache received an update event before getting populated from the db. Ignoring...");
							return;
						}
						idpMappers.remove(idpMapperUpdatedEvent.getIdentityProviderMapper());
						idpMappers.add(idpMapperUpdatedEvent.getIdentityProviderMapper());
						idpMappersCache.put(idpMapperUpdatedEvent.getRealmId(), idpMappers);
					});

					cluster.registerListener(IdpMapperRemovedEvent.EVENT_NAME, (ClusterEvent event) -> {
						IdpMapperRemovedEvent idpMapperRemovedEvent = (IdpMapperRemovedEvent) event;
						Set<IdentityProviderMapperModel> idpMappers = idpMappersCache.get(idpMapperRemovedEvent.getRealmId());
						if(idpMappers==null){
							log.info("Identity provider mapper cache received a delete event before getting populated from the db. Ignoring...");
							return;
						}
						idpMappers.remove(idpMapperRemovedEvent.getIdentityProviderMapper());
						idpMappersCache.put(idpMapperRemovedEvent.getRealmId(), idpMappers);
					});

					cluster.registerListener(InfinispanCacheIdpProviderFactory.IDP_MAPPERS_CLEAR_CACHE_EVENT, (ClusterEvent event) -> {
						idpMappersCache.clear();
					});

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
