package org.keycloak.models.cache;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class CacheIdpProviderSpi implements Spi {

	@Override
	public boolean isInternal() {
		return true;
	}

	@Override
	public String getName() {
		return "idpCache";
	}

	@Override
	public Class<? extends Provider> getProviderClass() {
		return CacheIdpProviderI.class;
	}

	@Override
	public Class<? extends ProviderFactory> getProviderFactoryClass() {
		return CacheIdpProviderFactoryI.class;
	}
	
	
}
