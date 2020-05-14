package org.keycloak.models.cache.infinispan;

import java.util.List;
import java.util.Set;

import org.jboss.logging.Logger;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakTransaction;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserProvider;
import org.keycloak.models.cache.CacheIdpProvider;

public class IdpCacheSession implements CacheIdpProvider {

	protected static final Logger logger = Logger.getLogger(IdpCacheSession.class);
	
    protected IdpCacheManager cache;
    protected KeycloakSession session;
    protected IdentityProviderProvider delegate;
    protected boolean transactionActive;
    protected boolean setRollbackOnly;
    protected final long startupRevision;
	
	
    public IdpCacheSession(IdpCacheManager cache, KeycloakSession session) {
        this.cache = cache;
        this.session = session;
        this.startupRevision = cache.getCurrentCounter();
//        session.getTransactionManager().enlistAfterCompletion(getTransaction());
    }
    
    
    
    
    
	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

	@Override
	public void clear() {
		// TODO Auto-generated method stub
	}

	@Override
	public void registerIdentityProviderInvalidation(String id, String alias, String realmId) {
		// TODO Auto-generated method stub
	}
	
	
	static String getIdentityProviderByAliasCacheKey(String alias, String realmId) {
        return realmId + ".identity-provider.query.by.alias." + alias;
    }
    
	
	
    public IdentityProviderProvider getIdentityProviderDelegate() {
//        if (!transactionActive) throw new IllegalStateException("Cannot access delegate without a transaction");
//        if (identityProviderDelegate != null) return identityProviderDelegate;
//        identityProviderDelegate = session.identityProviderLocalStorage();
//        return identityProviderDelegate;
    	return null;
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
