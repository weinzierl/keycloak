package org.keycloak.broker.saml.aggregate;

import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateMetadataStoreProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;

public class SAMLAggregateIdentityProviderConfig extends IdentityProviderModel {

  public static final String METADATA_URL = "metadataUrl";

  private static final long serialVersionUID = 1L;

  private KeycloakSessionFactory sessionFactory;

  public SAMLAggregateIdentityProviderConfig() {
    super();
  }

  public SAMLAggregateIdentityProviderConfig(IdentityProviderModel model) {
    super(model);
  }

  public String getMetadataUrl() {
    return getConfig().get(METADATA_URL);
  }

  public void setMetadataUrl(String metadataUrl) {
    getConfig().put(METADATA_URL, metadataUrl);
  }

  @Override
  public void validate(RealmModel realm) {
    SAMLAggregateMetadataStoreProvider provider = getMetadataProvider();
    provider.parseMetadata(realm, getAlias(), getMetadataUrl());
  }

  private SAMLAggregateMetadataStoreProvider getMetadataProvider() {
    return sessionFactory.create().getProvider(SAMLAggregateMetadataStoreProvider.class);
  }

  public void setSessionFactory(KeycloakSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }
}
