package org.keycloak.broker.saml.aggregate;

import static java.util.Objects.isNull;

import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateMetadataProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;

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
    
    if (isNull(getInternalId())) {
      setInternalId(KeycloakModelUtils.generateId());
    }

    SAMLAggregateMetadataProvider provider = getMetadataProvider();
    provider.parseMetadata(this);
  }

  private SAMLAggregateMetadataProvider getMetadataProvider() {
    return sessionFactory.create().getProvider(SAMLAggregateMetadataProvider.class);
  }

  public void setSessionFactory(KeycloakSessionFactory sessionFactory) {
    this.sessionFactory = sessionFactory;
  }
}
