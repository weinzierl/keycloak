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
		Set<String> resp = new HashSet<String>();
		getIdentityProviders(realm).stream().forEach(idp -> resp.add(idp.getProviderId()));
		return new ArrayList<>(resp);
	}
    
	
	@Override
	public Long countIdentityProviders(RealmModel realm) {
		return new Long(getIdentityProviders(realm).size());
    }
	
	
	@Override
	public List<IdentityProviderModel> getIdentityProviders(RealmModel realm) {
		CachedIdentityProviders cachedIdps = cache.get(realm.getId());
		if(cachedIdps != null) {
			return new ArrayList<IdentityProviderModel>(cachedIdps.getIdentityProviders().values());
		}
		else {
			List<IdentityProviderModel> identityProviders = getIdentityProviderDelegate().getIdentityProviders(realm);
			Map<String, IdentityProviderModel> identityProvidersMap = identityProviders.stream().collect(Collectors.toMap(idp -> idp.getInternalId(), idp -> idp ));
			CachedIdentityProviders cachedIdentityProviders = new CachedIdentityProviders(realm.getId(), identityProvidersMap);
			cache.put(realm.getId(), cachedIdentityProviders);
			return identityProviders;
		}

	}

	@Override
	public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults) {
		List<IdentityProviderModel> identityProviders = getIdentityProviders(realm).stream()
				.filter(idp -> {
					String name = idp.getDisplayName()==null ? "" : idp.getDisplayName();
					return name.toLowerCase().contains(keyword.toLowerCase()) || idp.getAlias().toLowerCase().contains(keyword.toLowerCase());
				})
				.collect(Collectors.toList());
		if(firstResult >= identityProviders.size())
			return new ArrayList<IdentityProviderModel>();
		Integer end = (firstResult + maxResults) > identityProviders.size() ? identityProviders.size() : (firstResult + maxResults);
		return identityProviders.subList(firstResult, end);
		
	}

	@Override
	public IdentityProviderModel getIdentityProviderById(String internalId) {
		IdentityProviderModel identityProviderModel = null;
		for(CachedIdentityProviders cidp : cache.values()) {
			IdentityProviderModel temp = cidp.getIdentityProviders().get(internalId);
			if(temp != null) identityProviderModel = temp;
		}
		return identityProviderModel;
	}

	@Override
	public IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias) {
		return getIdentityProviders(realm).stream().filter(idp -> idp.getAlias().equals(alias)).findAny().orElse(null);
	}

	
	
	
	@Override
	public void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		getIdentityProviderDelegate().addIdentityProvider(realm, identityProvider);
		
		CachedIdentityProviders cached = cache.get(realm.getId());
		if(cached == null)
			cached = new CachedIdentityProviders(realm.getId(), new HashMap<String, IdentityProviderModel>());
		cached.getIdentityProviders().put(identityProvider.getInternalId(), identityProvider);
		cache.put(realm.getId(), cached);
		//TODO: notify the other cluster nodes
	}


	@Override
	public void removeIdentityProviderByAlias(RealmModel realm, String alias) {
		getIdentityProviderDelegate().removeIdentityProviderByAlias(realm, alias);
		
		CachedIdentityProviders cachedIdps = cache.get(realm.getId());
		cachedIdps.getIdentityProviders().values().removeIf(idp -> idp.getAlias().equals(alias));
		cache.put(realm.getId(), cachedIdps);
		//TODO: inform cluster about this removal
		
	}
	
	
	
	@Override
	public void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		
		if(identityProvider == null) return;
		
		getIdentityProviderDelegate().updateIdentityProvider(realm, identityProvider);
		
		CachedIdentityProviders cachedIdps = cache.get(realm.getId());
		cachedIdps.getIdentityProviders().put(identityProvider.getInternalId(), identityProvider);
		cache.put(realm.getId(), cachedIdps);
		//TODO: inform cluster about this change
		
	}
	
	@Override
    public void saveFederationIdp(RealmModel realmModel, IdentityProviderModel idpModel) {
        getIdentityProviderDelegate().saveFederationIdp(realmModel, idpModel);

        CachedIdentityProviders cachedIdps = cache.get(realmModel.getId());
        if (cachedIdps == null)
            cachedIdps = new CachedIdentityProviders(realmModel.getId(), new HashMap<String, IdentityProviderModel>());
        cachedIdps.getIdentityProviders().put(idpModel.getInternalId(), idpModel);
        cache.put(realmModel.getId(), cachedIdps);
        // TODO: inform cluster about the addition

    }
	
	@Override
	public boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, String idpAlias) {
		
		boolean result = getIdentityProviderDelegate().removeFederationIdp(realmModel, idpfModel, idpAlias);
		if(result) {
			CachedIdentityProviders cachedIdps = cache.get(realmModel.getId());
			Entry<String, IdentityProviderModel> idpEntry = cachedIdps.getIdentityProviders().entrySet().stream().filter(entry -> entry.getValue().getAlias().equals(idpAlias)).findAny().orElse(null);
			if(idpEntry != null) {
				if(idpEntry.getValue().getFederations().size() == 1) //belongs to only one federation, this one, so remove it entirely from cache
					cachedIdps.getIdentityProviders().remove(idpEntry.getKey());
				else if(idpEntry.getValue().getFederations().size() > 1)
					idpEntry.getValue().getFederations().remove(idpfModel.getInternalId());
				else //means it's zero. should never happen normally
					logger.errorf("Cache inconsistency! Trying to remove from cache an identity provider (alias= %s) which does not belong to the expected federation (alias= %s)", idpAlias, idpfModel.getAlias());
			}
			else 
				logger.errorf("Cache inconsistency! Could not locate within the federation (alias = %s) the identity provider (alias = %s) to delete", idpfModel.getAlias(), idpAlias);
			cache.put(realmModel.getId(), cachedIdps);
			
			//TODO: inform all nodes about this change.
			
		}
		return result;
		
	}
	
	
	
	@Override
	public boolean isIdentityFederationEnabled(RealmModel realm) {
		return getIdentityProviders(realm).size() > 0;
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
