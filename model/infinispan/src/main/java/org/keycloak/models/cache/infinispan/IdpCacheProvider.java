package org.keycloak.models.cache.infinispan;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.infinispan.Cache;
import org.infinispan.notifications.Listener;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryCreated;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryModified;
import org.infinispan.notifications.cachelistener.annotation.CacheEntryRemoved;
import org.infinispan.notifications.cachelistener.event.CacheEntryCreatedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryModifiedEvent;
import org.infinispan.notifications.cachelistener.event.CacheEntryRemovedEvent;
import org.jboss.logging.Logger;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.cluster.ClusterListener;
import org.keycloak.cluster.ClusterProvider;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderProvider;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.cache.CacheIdpProviderI;
import org.keycloak.models.cache.infinispan.events.*;

public class IdpCacheProvider implements CacheIdpProviderI {

	protected static final Logger logger = Logger.getLogger(IdpCacheProvider.class);

	private Cache<String, Set<IdentityProviderModel>> cacheIdp;
	private Cache<String, Set<IdentityProviderMapperModel>> cacheIdpMappers;

	private ClusterProvider cluster;

    protected KeycloakSession session;
    protected IdentityProviderProvider identityProviderDelegate;


    public IdpCacheProvider(Cache<String, Set<IdentityProviderModel>> identityProvidersCache, Cache<String, Set<IdentityProviderMapperModel>> identityProviderMappersCache, KeycloakSession session) {
    	this.cacheIdp = identityProvidersCache;
        this.cacheIdpMappers = identityProviderMappersCache;
		this.session = session;
		this.cluster = session.getProvider(ClusterProvider.class);
    }


    @Override
    public void clear() {
        cacheIdp.clear();
    	cluster.notify(InfinispanCacheIdpProviderFactory.IDP_CLEAR_CACHE_EVENTS, new ClearCacheEvent(), true, ClusterProvider.DCNotify.ALL_DCS);
    }

	@Override
    public IdentityProviderProvider getIdentityProviderDelegate() {
        if (identityProviderDelegate != null) return identityProviderDelegate;
        identityProviderDelegate = session.identityProviderLocalStorage();
        return identityProviderDelegate;
    }


	@Deprecated
	@Override
    public void evict(RealmModel realm, IdentityProviderModel identityProvider) {
		//not used
    }

    @Deprecated
    @Override
    public void evict(RealmModel realm) {
        //not used
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
		Set<IdentityProviderModel> idps = cacheIdp.get(realm.getId());
		if(idps!=null) {
			return idps.stream().collect(Collectors.toList());
		}
		else {
			List<IdentityProviderModel> identityProviders = getIdentityProviderDelegate().getIdentityProviders(realm);
			synchronized(cacheIdp) {
				identityProviders = getIdentityProviderDelegate().getIdentityProviders(realm);
				cacheIdp.put(realm.getId(), identityProviders.stream().collect(Collectors.toCollection(HashSet::new)));
			}
			return identityProviders;
		}
	}

	@Override
	public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult, Integer maxResults) {
    	//TODO: narrow down the search to a query with a filter (maintain case insensitivity)
		final String lowercaseKeyword = keyword.toLowerCase();
		List<IdentityProviderModel> identityProviders = getIdentityProviders(realm).stream()
				.filter(idp -> {
					String name = idp.getDisplayName()==null ? "" : idp.getDisplayName();
					return name.toLowerCase().contains(lowercaseKeyword) || idp.getAlias().toLowerCase().contains(lowercaseKeyword);
				})
				.collect(Collectors.toList());
		if(firstResult >= identityProviders.size())
			return new ArrayList<IdentityProviderModel>();
		Integer end = (firstResult + maxResults) > identityProviders.size() ? identityProviders.size() : (firstResult + maxResults);
		return identityProviders.subList(firstResult, end);

	}

	@Override
	public IdentityProviderModel getIdentityProviderById(RealmModel realm, String internalId) {
    	return getIdentityProviders(realm).stream().filter(idp -> idp.getInternalId().equals(internalId)).findFirst().orElse(null);
	}

	@Override
	public IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias) {
		return getIdentityProviders(realm).stream().filter(idp -> idp.getAlias().equals(alias)).findFirst().orElse(null);
	}


	@Override
	public void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		getIdentityProviderDelegate().addIdentityProvider(realm, identityProvider);

		ensureIdpsLoaded(realm);
		synchronized (cacheIdp) {
			Set<IdentityProviderModel> idps = cacheIdp.get(realm.getId());
			idps.add(identityProvider);
			cacheIdp.put(realm.getId(), idps);
		}
		cluster.notify(IdpAddedEvent.EVENT_NAME, new IdpAddedEvent(realm.getId(), identityProvider), true, ClusterProvider.DCNotify.ALL_DCS );
	}


	@Override
	public void removeIdentityProviderByAlias(RealmModel realm, String alias) {

    	//remove from database idp and mappers
		getIdentityProviderDelegate().removeIdentityProviderByAlias(realm, alias);

		//remove idp mappers from the cache
		ensureMappersLoaded(realm);
		Set<IdentityProviderMapperModel> idpMappers = getIdentityProviderMappersByAlias(realm, alias);
		synchronized (cacheIdpMappers) {
			cacheIdpMappers.put(realm.getId(), cacheIdpMappers.get(realm.getId()).stream().filter(m -> !m.getIdentityProviderAlias().equals(alias)).collect(Collectors.toSet()));
		}
		idpMappers.forEach(idpMapper -> {
			cluster.notify(IdpMapperRemovedEvent.EVENT_NAME, new IdpMapperRemovedEvent(realm.getId(), idpMapper), true, ClusterProvider.DCNotify.ALL_DCS );
		});

		//remove the identity provider from the cache
		ensureIdpsLoaded(realm);
		IdentityProviderModel idpModel = getIdentityProviderByAlias(realm, alias);
		synchronized (cacheIdp) {
			Set<IdentityProviderModel> idps = cacheIdp.get(realm.getId());
			idps.remove(idpModel);
			cacheIdp.put(realm.getId(), idps);
		}
		cluster.notify(IdpRemovedEvent.EVENT_NAME, new IdpRemovedEvent(realm.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS );
	}

	@Override
	public void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
		if(identityProvider == null) return;
		getIdentityProviderDelegate().updateIdentityProvider(realm, identityProvider);

		ensureIdpsLoaded(realm);
		synchronized (cacheIdp) {
			Set<IdentityProviderModel> idps = cacheIdp.get(realm.getId());
			idps.remove(identityProvider);
			idps.add(identityProvider); //if already exists an element with the same internalId, it won't replace. That's why we first remove it.
			cacheIdp.put(realm.getId(), idps);
		}
		cluster.notify(IdpUpdatedEvent.EVENT_NAME, new IdpUpdatedEvent(realm.getId(), identityProvider), true, ClusterProvider.DCNotify.ALL_DCS );
	}


	@Override
	public void saveFederationIdp(RealmModel realmModel, IdentityProviderModel idpModel) {
		getIdentityProviderDelegate().saveFederationIdp(realmModel, idpModel);

		ensureIdpsLoaded(realmModel);
		synchronized (cacheIdp) {
			Set<IdentityProviderModel> idps = cacheIdp.get(realmModel.getId());
			idps.remove(idpModel); //need to remove the previous (see javadoc of Set collection why)
			idps.add(idpModel);
			cacheIdp.put(realmModel.getId(), idps);
		}
		cluster.notify(IdpUpdatedEvent.EVENT_NAME, new IdpUpdatedEvent(realmModel.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS );
	}


	@Override
	public boolean removeFederationIdp(RealmModel realmModel, IdentityProvidersFederationModel idpfModel, String idpAlias) {
		boolean result = getIdentityProviderDelegate().removeFederationIdp(realmModel, idpfModel, idpAlias);
		if(result) {
			IdentityProviderModel idpModel = getIdentityProviderByAlias(realmModel, idpAlias);
			if(idpModel != null) {
				if(idpModel.getFederations().size() == 1) { //belongs to only one federation, this one, so remove it entirely from cache
					ensureIdpsLoaded(realmModel);
					synchronized (cacheIdp) {
						cacheIdp.put(realmModel.getId(), cacheIdp.get(realmModel.getId()).stream().filter(i -> !i.getAlias().equals(idpAlias)).collect(Collectors.toSet()));
					}
					cluster.notify(IdpRemovedEvent.EVENT_NAME, new IdpRemovedEvent(realmModel.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS );
					//remove also its mappers
					ensureMappersLoaded(realmModel);
					Set<IdentityProviderMapperModel> idpMappers;
					synchronized (cacheIdpMappers) {
						idpMappers = cacheIdpMappers.get(realmModel.getId()).stream().filter(m -> !m.getIdentityProviderAlias().equals(idpAlias)).collect(Collectors.toSet());
						cacheIdpMappers.put(realmModel.getId(), idpMappers);
					}
					idpMappers.forEach(idpMapper -> {
						cluster.notify(IdpMapperRemovedEvent.EVENT_NAME, new IdpMapperRemovedEvent(realmModel.getId(), idpMapper), true, ClusterProvider.DCNotify.ALL_DCS );
					});
				}
				else if(idpModel.getFederations().size() > 1) {
					idpModel.getFederations().remove(idpfModel.getInternalId());
					ensureIdpsLoaded(realmModel);
					synchronized (cacheIdp) {
						Set<IdentityProviderModel> idps = cacheIdp.get(realmModel.getId());
						idps.remove(idpModel); //see java.util.Set javadoc why we remove before inserting
						idps.add(idpModel);
						cacheIdp.put(realmModel.getId(), idps);
					}
					cluster.notify(IdpUpdatedEvent.EVENT_NAME, new IdpUpdatedEvent(realmModel.getId(), idpModel), true, ClusterProvider.DCNotify.ALL_DCS );
				}
				else //means it's zero. should never happen normally
					logger.errorf("Cache inconsistency! Trying to remove from cache an identity provider (alias= %s) which does not belong to the expected federation (alias= %s)", idpAlias, idpfModel.getAlias());
			}
			else
				logger.errorf("Cache inconsistency! Could not locate within the federation (alias = %s) the identity provider (alias = %s) to delete", idpfModel.getAlias(), idpAlias);
		}
		return result;
	}

	@Override
	public boolean isIdentityFederationEnabled(RealmModel realm) {
    	return cacheIdp.get(realm.getId()).size() > 0;
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {
    	Set<IdentityProviderMapperModel> mappers = cacheIdpMappers.get(realmModel.getId());
    	if(mappers == null) {
    		//get from database
    		mappers = getIdentityProviderDelegate().getIdentityProviderMappers(realmModel);
    		cacheIdpMappers.put(realmModel.getId(), mappers);
		}
    	return mappers;
	}

	@Override
	public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel, String brokerAlias) {
    	return getIdentityProviderMappers(realmModel).stream().filter(m -> m.getIdentityProviderAlias().equals(brokerAlias)).collect(Collectors.toSet());
	}

	@Override
	public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel model) {
	    //add to db
    	model = getIdentityProviderDelegate().addIdentityProviderMapper(realmModel, model);
    	//add to cache
		ensureMappersLoaded(realmModel);
		synchronized (cacheIdpMappers) {
			Set<IdentityProviderMapperModel> cachedMappers = cacheIdpMappers.get(realmModel.getId());
			cachedMappers.add(model);
			cacheIdpMappers.put(realmModel.getId(), cachedMappers);
		}
		cluster.notify(IdpMapperAddedEvent.EVENT_NAME, new IdpMapperAddedEvent(realmModel.getId(), model), true, ClusterProvider.DCNotify.ALL_DCS );
	    return model;
	}

	@Override
	public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapper) {
	    getIdentityProviderDelegate().removeIdentityProviderMapper(realmModel, mapper);
		ensureMappersLoaded(realmModel);
		synchronized (cacheIdpMappers) {
			Set<IdentityProviderMapperModel> cachedMappers = cacheIdpMappers.get(realmModel.getId());
			cachedMappers.remove(mapper);
			cacheIdpMappers.put(realmModel.getId(), cachedMappers);
		}
		cluster.notify(IdpMapperRemovedEvent.EVENT_NAME, new IdpMapperRemovedEvent(realmModel.getId(), mapper), true, ClusterProvider.DCNotify.ALL_DCS );
	}

	@Override
	public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapper) {
	    if(mapper == null) return;
        getIdentityProviderDelegate().updateIdentityProviderMapper(realmModel, mapper);
		ensureMappersLoaded(realmModel);
		synchronized (cacheIdpMappers) {
			Set<IdentityProviderMapperModel> cachedMappers = cacheIdpMappers.get(realmModel.getId());
			cachedMappers.remove(mapper); //see java.util.Set javadoc why we remove before inserting
			cachedMappers.add(mapper);
			cacheIdpMappers.put(realmModel.getId(), cachedMappers);
		}
		cluster.notify(IdpMapperUpdatedEvent.EVENT_NAME, new IdpMapperUpdatedEvent(realmModel.getId(), mapper), true, ClusterProvider.DCNotify.ALL_DCS );
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id) {
		return getIdentityProviderMappers(realmModel).stream().filter(m -> m.getId().equals(id)).findFirst().orElse(null);
	}

	@Override
	public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias, String name) {
		return getIdentityProviderMappers(realmModel).stream().filter(m -> m.getIdentityProviderAlias().equals(alias) && m.getName().equals(name)).findFirst().orElse(null);
	}


	private void ensureIdpsLoaded(RealmModel realm){
		if(cacheIdp.get(realm.getId())==null)
			getIdentityProviders(realm);
	}

	private void ensureMappersLoaded(RealmModel realm){
		if(cacheIdpMappers.get(realm.getId())==null)
			getIdentityProviderMappers(realm);
	}

}
