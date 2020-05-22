package org.keycloak.models.cache;


import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProviderProvider;
import org.keycloak.models.RealmModel;

public interface CacheIdpProvider extends IdentityProviderProvider {

    /**
     * Evict an identity provider from cache.
     *
     * @param user
     */
    void evict(RealmModel realm, IdentityProviderModel user);

    /**
     * Evict all identity providers of a specific realm
     *
     * @param realm
     */
    void evict(RealmModel realm);

    /**
     * Clear cache entirely.
     *
     */
    void clear();
	
    IdentityProviderProvider getIdentityProviderDelegate();
    
//    void registerIdentityProviderInvalidation(String id, String alias, String realmId);
    
}
