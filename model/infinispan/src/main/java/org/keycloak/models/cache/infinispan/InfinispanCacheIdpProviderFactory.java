package org.keycloak.models.cache.infinispan;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.cache.CacheIdpProvider;
import org.keycloak.models.cache.CacheIdpProviderFactory;
import org.keycloak.models.cache.CacheRealmProviderFactory;

public class InfinispanCacheIdpProviderFactory implements CacheIdpProviderFactory {

	@Override
	public CacheIdpProvider create(KeycloakSession session) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void init(Scope config) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getId() {
		return "default";
	}

}
