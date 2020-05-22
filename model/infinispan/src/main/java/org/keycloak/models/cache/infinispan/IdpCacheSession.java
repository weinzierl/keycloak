package org.keycloak.models.cache.infinispan;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.broker.provider.IdentityProvider;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CacheIdpProvider;
import org.keycloak.models.cache.infinispan.events.IdentityProviderRemovedEvent;
import org.keycloak.models.cache.infinispan.events.IdentityProvidersRealmRemovedEvent;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;

//TODO: Fill in the appropriate code to use the cache before using the getIdentityProviderDelegate(), just like as in the UserCacheSession.
public class IdpCacheSession implements CacheIdpProvider {

	protected static final Logger logger = Logger.getLogger(IdpCacheSession.class);
	
//	public static final String IDP_MAPPERS_QUERY_SUFFIX = ".idp.mappers";
	
    protected IdpCacheManager cache;
    protected KeycloakSession session;
    protected IdentityProviderProvider identityProviderDelegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;
    protected final long startupRevision;
	
    
    protected Map<String, IdentityProviderModel> managedIdps = new HashMap<>(); //keys are realmIds
//    protected Map<String, IdentityProviderMapperModel> managedIdpMappers = new HashMap<>();
    
//    protected Map<String, Set<String>> invalidations = new HashMap<String,Set<String>>();
    
    protected Set<String> invalidations = new HashSet<>();
    protected Set<String> realmInvalidations = new HashSet<>();
    
    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster
    
    
    public IdpCacheSession(IdpCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        this.startupRevision = cache.getCurrentCounter();
        session.getTransactionManager().enlistAfterCompletion(getTransaction());
    }
    
    @Override
    public void clear() {
    	cache.clear();
    	ClusterProvider cluster = session.getProvider(ClusterProvider.class);
    	cluster.notify(InfinispanCacheIdpProviderFactory.IDP_CLEAR_CACHE_EVENTS, new ClearCacheEvent(), true, ClusterProvider.DCNotify.ALL_DCS);
    }
    
	@Override
    public IdentityProviderProvider getIdentityProviderDelegate() {
        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
        if (identityProviderDelegate != null) return identityProviderDelegate;
        identityProviderDelegate = session.identityProviderLocalStorage();
        return identityProviderDelegate;
    }
	
	
	@Override
    public void evict(RealmModel realm, IdentityProviderModel identityProvider) {
        if (!transactionActive) throw new IllegalStateException("Cannot call evict() without a transaction");
        getIdentityProviderDelegate(); // invalidations need delegate set
        cache.identityProviderRemoved(identityProvider.getInternalId(), identityProvider.getAlias(), realm.getId(), invalidations);
        invalidationEvents.add(IdentityProviderRemovedEvent.create(identityProvider.getInternalId(), identityProvider.getAlias(), realm.getId()));
    }
	


    @Override
    public void evict(RealmModel realm) {
        addRealmInvalidation(realm.getId());
    }
	
	
    private void addRealmInvalidation(String realmId) {
        realmInvalidations.add(realmId);
        invalidationEvents.add(IdentityProvidersRealmRemovedEvent.create(realmId));
    }
    
    
    protected void runInvalidations() {
        for (String realmId : realmInvalidations) {
            cache.invalidateRealmIdentityProviders(realmId, invalidations);
        }
        for (String invalidation : invalidations) {
            cache.invalidateObject(invalidation);
        }

        cache.sendInvalidationEvents(session, invalidationEvents, InfinispanUserCacheProviderFactory.USER_INVALIDATION_EVENTS);
    }
    
    
    private KeycloakTransaction getTransaction() {
        return new KeycloakTransaction() {
            @Override
            public void begin() {
                transactionActive = true;
            }

            @Override
            public void commit() {
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void rollback() {
                setRollbackOnly = true;
                runInvalidations();
                transactionActive = false;
            }

            @Override
            public void setRollbackOnly() {
                setRollbackOnly = true;
            }

            @Override
            public boolean getRollbackOnly() {
                return setRollbackOnly;
            }

            @Override
            public boolean isActive() {
                return transactionActive;
            }
        };
    }
    
    
	@Override
	public void close() {
		if (identityProviderDelegate != null) identityProviderDelegate.close();
	}

	
	
	static String getIdentityProviderByAliasCacheKey(String alias, String realmId) {
        return realmId + ".idpAlias." + alias;
    }
    
	static String getIdentityProviderByIdCacheKey(String id, String realmId) {
        return realmId + ".idpId." + id;
    }
	

	
	@Override
	public List<String> getUsedIdentityProviderIdTypes(RealmModel realm) {
		return getIdentityProviderDelegate().getUsedIdentityProviderIdTypes(realm);
	}
    
	@Override
	public Long countIdentityProviders(RealmModel realm) {
		return getIdentityProviderDelegate().countIdentityProviders(realm);
    }
	
	
	@Override
	public List<IdentityProviderModel> getIdentityProviders(RealmModel realm) {
		return getIdentityProviderDelegate().getIdentityProviders(realm);
	}

	@Override
	public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults) {
		return getIdentityProviderDelegate().searchIdentityProviders(realm, keyword, firstResult, maxResults);
	}

	@Override
	public IdentityProviderModel getIdentityProviderById(String internalId) {
		return getIdentityProviderDelegate().getIdentityProviderById(internalId);
	}

	@Override
	public IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias) {
		return getIdentityProviderDelegate().getIdentityProviderByAlias(realm, alias);
	}

	
	
	
	@Override
	public void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		getIdentityProviderDelegate().addIdentityProvider(realm, identityProvider);
//        addedIdentityProvider(realm, identityProvider);
	}

//	private IdentityProviderModel addedIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
//        logger.trace("added identity provider.....");
//
//        invalidateIdentityProvider(identityProvider.getInternalId());
//        // this is needed so that an identity provider that hasn't been committed isn't cached in a query
//        listInvalidations.add(realm.getId());
//
////        invalidationEvents.add(IdentityProviderAddedEvent.create(identityProvider.getInternalId(), identityProvider.getAlias(), realm.getId()));
//        cache.identityProviderAdded(realm.getId(), identityProvider.getInternalId(), identityProvider.getAlias(), invalidations);
//        return identityProvider;
//    }
	
	
//    private void invalidateIdentityProvider(String id) {
//        invalidations.add(id);
//        IdentityProviderProvider adapter = managedIdentityProviders.get(id);
//        if (adapter != null && adapter instanceof IdentityProviderAdapter) ((IdentityProviderAdapter)adapter).invalidate();
//    }
	

	@Override
	public void removeIdentityProviderByAlias(RealmModel realm, String alias) {
		
		IdentityProviderModel identityProvider = getIdentityProviderByAlias(realm, alias);
		if(identityProvider == null) return;
//		
//		invalidateIdentityProvider(identityProvider.getInternalId());
//		// this is needed so that an identity provider that hasn't been committed isn't cached in a query
//		listInvalidations.add(realm.getId());
//		
//		invalidationEvents.add(IdentityProviderRemovedEvent.create(identityProvider, realm.getId()));
//		cache.identityProviderRemoval(realm.getId(), identityProvider.getInternalId(), identityProvider.getAlias(), invalidations);
		
		getIdentityProviderDelegate().removeIdentityProviderByAlias(realm, alias);
		
	}
	
	
	@Override
	public void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		
		if(identityProvider == null) return;
		
//		invalidateIdentityProvider(identityProvider.getInternalId());
//		
//		listInvalidations.add(realm.getId());
//		
////		invalidationEvents.add(IdentityProviderUpdatedEvent.create(identityProvider.getInternalId(), identityProvider.getAlias(), realm.getId()));
//		cache.identityProviderUpdated(realm.getId(), identityProvider.getInternalId(), identityProvider.getAlias(), invalidations);
		
		getIdentityProviderDelegate().updateIdentityProvider(realm, identityProvider);
		
	}
	
	
	@Override
	public boolean isIdentityFederationEnabled(RealmModel realm) {
		return getIdentityProviderDelegate().isIdentityFederationEnabled(realm);
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {
		return getIdentityProviderDelegate().getIdentityProviderMappers(realmModel);
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias) {
		return getIdentityProviderDelegate().getIdentityProviderMappersByAlias(realmModel, brokerAlias);
	}

	@Override
	public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model) {
		return getIdentityProviderDelegate().addIdentityProviderMapper(realmModel, model);
	}

	@Override
	public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
		getIdentityProviderDelegate().removeIdentityProviderMapper(realmModel, mapping);
	}

	@Override
	public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
		getIdentityProviderDelegate().updateIdentityProviderMapper(realmModel, mapping);
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id) {
		return getIdentityProviderDelegate().getIdentityProviderMapperById(realmModel, id);
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name) {
		return getIdentityProviderDelegate().getIdentityProviderMapperByName(realmModel, alias, name);
	}

}
