package org.keycloak.models.cache.infinispan.entities;

import java.util.Map;

import org.keycloak.models.IdentityProviderModel;

public class CachedIdentityProviders {

	Map<String, IdentityProviderModel> identityProviders;
	String realmId;
	
	public CachedIdentityProviders(String realmId, Map<String, IdentityProviderModel> identityProviders) {
		this.realmId = realmId;
		this.identityProviders = identityProviders;
	}

	public Map<String, IdentityProviderModel> getIdentityProviders() {
		return identityProviders;
	}

	public String getRealmId() {
		return realmId;
	}

	
}
