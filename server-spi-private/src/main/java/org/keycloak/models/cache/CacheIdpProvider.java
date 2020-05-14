package org.keycloak.models.cache;


import org.keycloak.models.IdentityProviderProvider;

public interface CacheIdpProvider extends IdentityProviderProvider {

	void clear();

    void registerIdentityProviderInvalidation(String id, String alias, String realmId);
	
}
