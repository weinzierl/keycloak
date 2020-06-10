package org.keycloak.broker.saml.federation.metadata;

import org.keycloak.provider.Provider;
import org.keycloak.provider.ProviderFactory;
import org.keycloak.provider.Spi;

public class SAMLAggregateMetadataSpi implements Spi {

  public static final String NAME = "saml-aggregate-metadata";

  @Override
  public boolean isInternal() {
    return false;
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  public Class<? extends Provider> getProviderClass() {
    return SAMLAggregateMetadataProvider.class;
  }

  @SuppressWarnings("rawtypes")
  @Override
  public Class<? extends ProviderFactory> getProviderFactoryClass() {
    return SAMLAggregateMetadataProviderFactory.class;
  }



}
