//package org.keycloak.models.cache.infinispan;
//
//import java.util.List;
//import java.util.Set;
//
//import org.keycloak.models.IdentityProviderMapperModel;
//import org.keycloak.models.IdentityProviderModel;
//import org.keycloak.models.IdentityProviderProvider;
//import org.keycloak.models.KeycloakSession;
//import org.keycloak.models.RealmModel;
//import org.keycloak.models.cache.infinispan.entities.CachedIdentityProvider;
//
//public class IdentityProviderAdapter extends IdentityProviderModel {
//
//	protected CachedIdentityProvider cached;
//    protected IdpCacheSession cacheSession;
//    protected volatile RealmModel updated;
//    protected KeycloakSession session;
//    
//    
//    
//	@Override
//	public void close() {
//		// TODO Auto-generated method stub
//		
//	}
//	@Override
//	public List<String> getUsedIdentityProviderIdTypes(RealmModel realm) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public Long countIdentityProviders(RealmModel realm) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public List<IdentityProviderModel> getIdentityProviders(RealmModel realm) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public List<IdentityProviderModel> searchIdentityProviders(RealmModel realm, String keyword, Integer firstResult,
//			Integer maxResults) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public IdentityProviderModel getIdentityProviderById(String internalId) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public IdentityProviderModel getIdentityProviderByAlias(RealmModel realm, String alias) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public void addIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
//		// TODO Auto-generated method stub
//		
//	}
//	@Override
//	public void removeIdentityProviderByAlias(RealmModel realm, String alias) {
//		// TODO Auto-generated method stub
//		
//	}
//	@Override
//	public void updateIdentityProvider(RealmModel realm, IdentityProviderModel identityProvider) {
//		// TODO Auto-generated method stub
//		
//	}
//	@Override
//	public boolean isIdentityFederationEnabled(RealmModel realm) {
//		// TODO Auto-generated method stub
//		return false;
//	}
//	@Override
//	public Set<IdentityProviderMapperModel> getIdentityProviderMappers(RealmModel realmModel) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public Set<IdentityProviderMapperModel> getIdentityProviderMappersByAlias(RealmModel realmModel,
//			String brokerAlias) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public IdentityProviderMapperModel addIdentityProviderMapper(RealmModel realmModel,
//			IdentityProviderMapperModel model) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public void removeIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
//		// TODO Auto-generated method stub
//		
//	}
//	@Override
//	public void updateIdentityProviderMapper(RealmModel realmModel, IdentityProviderMapperModel mapping) {
//		// TODO Auto-generated method stub
//		
//	}
//	@Override
//	public IdentityProviderMapperModel getIdentityProviderMapperById(RealmModel realmModel, String id) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	@Override
//	public IdentityProviderMapperModel getIdentityProviderMapperByName(RealmModel realmModel, String alias,
//			String name) {
//		// TODO Auto-generated method stub
//		return null;
//	}
//	
//	
//}
