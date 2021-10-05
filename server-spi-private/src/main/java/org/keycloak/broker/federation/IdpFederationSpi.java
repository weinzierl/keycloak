package org.keycloak.broker.federation;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class IdpFederationSpi implements Spi {

    public static final String IDP_FEDERATION_SPI_NAME = "idp-federation-spi";

    @Override
    public boolean isInternal() {
        return true;
    }

    @Override
    public String getName() {
        return IDP_FEDERATION_SPI_NAME;
    }

    @Override
    public Class<? extends Provider> getProviderClass() {
        return IdpFederationProvider.class;
    }

    @Override
    public Class<? extends ProviderFactory> getProviderFactoryClass() {
        return IdpFederationProviderFactory.class;
    }
}