package org.keycloak.models.cache.infinispan;

import java.util.*;
import java.util.Map.Entry;
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
import org.keycloak.models.cache.infinispan.entities.CachedIdentityProviderMappers;
import org.keycloak.models.cache.infinispan.entities.CachedIdentityProviders;
import org.keycloak.models.cache.infinispan.events.InvalidationEvent;


public class IdpCacheProvider implements CacheIdpProviderI {

	protected static final Logger logger = Logger.getLogger(IdpCacheProvider.class);
	
	
    protected Cache<String, CachedIdentityProviders> cacheIdp;
    protected Cache<String, CachedIdentityProviderMappers> cacheIdpMappers;
    protected KeycloakSession session;
    protected IdentityProviderProvider identityProviderDelegate;
    
    
    protected Set<String> invalidations = new HashSet<>();
    protected Set<String> realmInvalidations = new HashSet<>();
    
//    protected Set<InvalidationEvent> invalidationEvents = new HashSet<>(); // Events to be sent across cluster
    
    
    public IdpCacheProvider(Cache<String, CachedIdentityProviders> identityProvidersCache, Cache<String, CachedIdentityProviderMappers> identityProviderMappersCache, KeycloakSession session) {
        this.cacheIdp = identityProvidersCache;
        this.cacheIdpMappers = identityProviderMappersCache;
        this.session = session;
    }
    
    
    public Cache<String, CachedIdentityProviders> getCache(){
    	return cacheIdp;
    }
    
    
    @Override
    public void clear() {
        cacheIdp.clear();
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
		CachedIdentityProviders cachedIdps = (CachedIdentityProviders) cacheIdp.get(realm.getId());
		cachedIdps.getIdentityProviders().remove(identityProvider.getInternalId());
		cachedIdps = new CachedIdentityProviders(realm.getId(), cachedIdps.getIdentityProviders());
		cacheIdp.put(realm.getId(), cachedIdps);
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
		CachedIdentityProviders cachedIdps = cacheIdp.get(realm.getId());
		if(cachedIdps != null) {
			return new ArrayList<IdentityProviderModel>(cachedIdps.getIdentityProviders().values());
		}
		else {
			List<IdentityProviderModel> identityProviders = getIdentityProviderDelegate().getIdentityProviders(realm);
			Map<String, IdentityProviderModel> identityProvidersMap = identityProviders.stream().collect(Collectors.toMap(idp -> idp.getInternalId(), idp -> idp ));
			CachedIdentityProviders cachedIdentityProviders = new CachedIdentityProviders(realm.getId(), identityProvidersMap);
			cacheIdp.put(realm.getId(), cachedIdentityProviders);
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
		for(CachedIdentityProviders cidp : cacheIdp.values()) {
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
		
		CachedIdentityProviders cached = cacheIdp.get(realm.getId());
		if(cached == null)
			cached = new CachedIdentityProviders(realm.getId(), new HashMap<String, IdentityProviderModel>());
		cached.getIdentityProviders().put(identityProvider.getInternalId(), identityProvider);
		cacheIdp.put(realm.getId(), cached);
	}


	@Override
	public void removeIdentityProviderByAlias(RealmModel realm, String alias) {
		getIdentityProviderDelegate().removeIdentityProviderByAlias(realm, alias);

		//remove idp mappers from the cache
		Set<String> idpMapperIds = getIdentityProviderMappersByAlias(realm, alias).stream().map(mapper -> mapper.getId()).collect(Collectors.toSet());
		CachedIdentityProviderMappers cachedIdpMappers = cacheIdpMappers.get(realm.getId());
		for(Iterator<String> iterator = cachedIdpMappers.getIdentityProviderMappers().keySet().iterator(); iterator.hasNext(); ) {
			String key = iterator.next();
			if(idpMapperIds.contains(key))
				iterator.remove();
		}
		cacheIdpMappers.put(realm.getId(), cachedIdpMappers);

		//remove the identity provider from the cache
		CachedIdentityProviders cachedIdps = cacheIdp.get(realm.getId());
		cachedIdps.getIdentityProviders().values().removeIf(idp -> idp.getAlias().equals(alias));
		cacheIdp.put(realm.getId(), cachedIdps);
	}
	
	
	
	@Override
	public void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		
		if(identityProvider == null) return;
		
		getIdentityProviderDelegate().updateIdentityProvider(realm, identityProvider);
		
		CachedIdentityProviders cachedIdps = cacheIdp.get(realm.getId());
		cachedIdps.getIdentityProviders().put(identityProvider.getInternalId(), identityProvider);
		cacheIdp.put(realm.getId(), cachedIdps);
		
	}

	@Override
	public void saveFederationIdp(RealmModel realmModel, IdentityProviderModel idpModel) {
		getIdentityProviderDelegate().saveFederationIdp(realmModel, idpModel);

		CachedIdentityProviders cachedIdps = cacheIdp.get(realmModel.getId());
		if (cachedIdps == null)
			cachedIdps = new CachedIdentityProviders(realmModel.getId(), new HashMap<String, IdentityProviderModel>());
		cachedIdps.getIdentityProviders().put(idpModel.getInternalId(), idpModel);
		cacheIdp.put(realmModel.getId(), cachedIdps);
		// TODO: inform cluster about the addition

	}
	
	@Override
	public boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, String idpAlias) {
		
		boolean result = getIdentityProviderDelegate().removeFederationIdp(realmModel, idpfModel, idpAlias);
		if(result) {
			CachedIdentityProviders cachedIdps = cacheIdp.get(realmModel.getId());
			Entry<String, IdentityProviderModel> idpEntry = cachedIdps.getIdentityProviders().entrySet().stream().filter(entry -> entry.getValue().getAlias().equals(idpAlias)).findAny().orElse(null);
			if(idpEntry != null) {
				if(idpEntry.getValue().getFederations().size() == 1) { //belongs to only one federation, this one, so remove it entirely from cache
					cachedIdps.getIdentityProviders().remove(idpEntry.getKey());
					//remove also its mappers
					Set<String> idpMapperIds = getIdentityProviderMappersByAlias(realmModel, idpAlias).stream().map(mapper -> mapper.getId()).collect(Collectors.toSet());
					CachedIdentityProviderMappers cachedIdpMappers = cacheIdpMappers.get(realmModel.getId());
					for(Iterator<String> iterator = cachedIdpMappers.getIdentityProviderMappers().keySet().iterator(); iterator.hasNext(); ) {
						String key = iterator.next();
						if(idpMapperIds.contains(key))
							iterator.remove();
					}
					cacheIdpMappers.put(realmModel.getId(), cachedIdpMappers);
				}
				else if(idpEntry.getValue().getFederations().size() > 1)
					idpEntry.getValue().getFederations().remove(idpfModel.getInternalId());
				else //means it's zero. should never happen normally
					logger.errorf("Cache inconsistency! Trying to remove from cache an identity provider (alias= %s) which does not belong to the expected federation (alias= %s)", idpAlias, idpfModel.getAlias());
			}
			else 
				logger.errorf("Cache inconsistency! Could not locate within the federation (alias = %s) the identity provider (alias = %s) to delete", idpfModel.getAlias(), idpAlias);
			cacheIdp.put(realmModel.getId(), cachedIdps);
			
		}
		return result;
		
	}
	
	
	
	@Override
	public boolean isIdentityFederationEnabled(RealmModel realm) {
		return getIdentityProviders(realm).size() > 0;
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {

	    CachedIdentityProviderMappers cachedIdpMappers = cacheIdpMappers.get(realmModel.getId());
        if(cachedIdpMappers != null) {
			return cachedIdpMappers.getIdentityProviderMappers().values().stream().collect(Collectors.toSet());
        }
        else {
            Set<IdentityProviderMapperModel> identityProviderMappers = getIdentityProviderDelegate().getIdentityProviderMappers(realmModel);
            Map<String, IdentityProviderMapperModel> identityProviderMappersMap = identityProviderMappers.stream().collect(Collectors.toMap(idpM -> idpM.getId(), idpM -> idpM ));
            CachedIdentityProviderMappers cachedIdentityProviderMappers = new CachedIdentityProviderMappers(realmModel.getId(), identityProviderMappersMap);
            cacheIdpMappers.put(realmModel.getId(), cachedIdentityProviderMappers);
            return identityProviderMappers;
        }
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias) {
	    return getIdentityProviderMappers(realmModel).stream().filter(idpM -> idpM.getIdentityProviderAlias().equals(brokerAlias)).collect(Collectors.toSet());
	}

	@Override
	public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model) {
	    IdentityProviderMapperModel idpModel = getIdentityProviderDelegate().addIdentityProviderMapper(realmModel, model);

	    CachedIdentityProviderMappers cached = cacheIdpMappers.get(realmModel.getId());
	    if(cached == null)
	        cached = new CachedIdentityProviderMappers(realmModel.getId(), new HashMap<String, IdentityProviderMapperModel>());
	    cached.getIdentityProviderMappers().put(idpModel.getId(), idpModel);
	    cacheIdpMappers.put(realmModel.getId(), cached);
	    return idpModel;
	}

	@Override
	public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapper) {
	    getIdentityProviderDelegate().removeIdentityProviderMapper(realmModel, mapper);

	    CachedIdentityProviderMappers cached = cacheIdpMappers.get(realmModel.getId());
	    cached.getIdentityProviderMappers().values().removeIf(m -> m.getId().equals(mapper.getId()));
	    cacheIdpMappers.put(realmModel.getId(), cached);
	}

	@Override
	public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapper) {
	    if(mapper == null) return;
        getIdentityProviderDelegate().updateIdentityProviderMapper(realmModel, mapper);

        CachedIdentityProviderMappers cachedIdpMappers = cacheIdpMappers.get(realmModel.getId());
        cachedIdpMappers.getIdentityProviderMappers().put(mapper.getId(), mapper);
        cacheIdpMappers.put(realmModel.getId(), cachedIdpMappers);
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id) {
	    CachedIdentityProviderMappers cachedIdpMappers = cacheIdpMappers.get(realmModel.getId());
	    if(cachedIdpMappers != null)
	        return cachedIdpMappers.getIdentityProviderMappers().get(id);
	    else
	        return getIdentityProviderDelegate().getIdentityProviderMapperById(realmModel, id);
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name) {
	    CachedIdentityProviderMappers cachedIdpMappers = cacheIdpMappers.get(realmModel.getId());
	    if(cachedIdpMappers != null)
	        return cachedIdpMappers.getIdentityProviderMappers().values().stream().filter(idpMapper -> idpMapper.getIdentityProviderAlias().equals(alias) && idpMapper.getName().equals(name)).findAny().orElse(null);
	    else
	        return getIdentityProviderDelegate().getIdentityProviderMapperByName(realmModel, alias, name);

	}

}
