package org.keycloak.models.cache.infinispan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;

import org.infinispan.Cache;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderProvider;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CacheIdpProviderI;
import org.keycloak.models.cache.infinispan.entities.CachedIdentityProviders;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;


public class IdpCacheProvider implements CacheIdpProviderI {

	protected static final Logger logger = Logger.getLogger(IdpCacheProvider.class);
	
	
    protected Cache<String, CachedIdentityProviders> cache;
    protected KeycloakSession session;
    protected IdentityProviderProvider identityProviderDelegate;
    
    
    protected Set<String> invalidations = new HashSet<>();
    protected Set<String> realmInvalidations = new HashSet<>();
    
//    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster
    
    
    public IdpCacheProvider(Cache<String, CachedIdentityProviders> cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
    }
    
    
    public Cache<String, CachedIdentityProviders> getCache(){
    	return cache;
    }
    
    
    @Override
    public void clear() {
    	cache.clear();
    	ClusterProvider cluster = session.getProvider(ClusterProvider.class);
    	cluster.notify(InfinispanCacheIdpProviderFactory.IDP_CLEAR_CACHE_EVENTS, new ClearCacheEvent(), true, ClusterProvider.DCNotify.ALL_DCS);
    }
    
	@Override
    public IdentityProviderProvider getIdentityProviderDelegate() {
        if (identityProviderDelegate != null) return identityProviderDelegate;
        identityProviderDelegate = session.identityProviderLocalStorage();
        return identityProviderDelegate;
    }
	
	
	@Override
    public void evict(RealmModel realm, IdentityProviderModel identityProvider) {
		CachedIdentityProviders cachedIdps = (CachedIdentityProviders) cache.get(realm.getId());
		cachedIdps.getIdentityProviders().remove(identityProvider.getInternalId());
		cachedIdps = new CachedIdentityProviders(realm.getId(), cachedIdps.getIdentityProviders());
		cache.put(realm.getId(), cachedIdps);
		//TODO: inform cluster about this eviction
    }
	
	

    @Override
    public void evict(RealmModel realm) {
//        addRealmInvalidation(realm.getId());
    }
	
    
	@Override
	public void close() {
		if (identityProviderDelegate != null) identityProviderDelegate.close();
	}
	

	@Override
	public List<String> getUsedIdentityProviderIdTypes(RealmModel realm) {
		return session.identityProviderLocalStorage().getUsedIdentityProviderIdTypes(realm);
	}
    
	
	@Override
	public Long countIdentityProviders(RealmModel realm) {
		return new Long(getIdentityProviders(realm).size());
    }
	
	
	@Override
	public List<IdentityProviderModel> getIdentityProviders(RealmModel realm) {
		return session.identityProviderLocalStorage().getIdentityProviders(realm);
	}

	@Override
	public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults) {
		return session.identityProviderLocalStorage().searchIdentityProviders(realm, keyword, firstResult, maxResults);
	}

	@Override
	public IdentityProviderModel getIdentityProviderById(String internalId) {
		return session.identityProviderLocalStorage().getIdentityProviderById(internalId);
	}

	@Override
	public IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias) {
		return session.identityProviderLocalStorage().getIdentityProviderByAlias(realm, alias);
	}

	
	
	
	@Override
	public void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		session.identityProviderLocalStorage().addIdentityProvider(realm, identityProvider);
	}


	@Override
	public void removeIdentityProviderByAlias(RealmModel realm, String alias) {
		session.identityProviderLocalStorage().removeIdentityProviderByAlias(realm, alias);
	}
	
	
	
	@Override
	public void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		session.identityProviderLocalStorage().updateIdentityProvider(realm, identityProvider);
	}
	
	@Override
    public boolean addFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, IdentityProviderModel idpModel) {
		return session.identityProviderLocalStorage().addFederationIdp(realmModel, idpfModel, idpModel);
	}
	
	@Override
	public boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, String idpAlias) {
		return session.identityProviderLocalStorage().removeFederationIdp(realmModel, idpfModel, idpAlias);
	}
	
	
	
	@Override
	public boolean isIdentityFederationEnabled(RealmModel realm) {
		return getIdentityProviders(realm).size() > 0;
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {
		return session.identityProviderLocalStorage().getIdentityProviderMappers(realmModel);
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias) {
		return session.identityProviderLocalStorage().getIdentityProviderMappersByAlias(realmModel, brokerAlias);
	}

	@Override
	public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model) {
		return session.identityProviderLocalStorage().addIdentityProviderMapper(realmModel, model);
	}

	@Override
	public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
		session.identityProviderLocalStorage().removeIdentityProviderMapper(realmModel, mapping);
	}

	@Override
	public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
		session.identityProviderLocalStorage().updateIdentityProviderMapper(realmModel, mapping);
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id) {
		return session.identityProviderLocalStorage().getIdentityProviderMapperById(realmModel, id);
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name) {
		return session.identityProviderLocalStorage().getIdentityProviderMapperByName(realmModel, alias, name);
	}

}
