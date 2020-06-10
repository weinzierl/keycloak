package org.keycloak.broker.saml.federation;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;

public class SAMLAggregateIdentityProviderConfig extends IdentityProviderModel {

  public static final String METADATA_URL = "metadataUrl";

  private static final long serialVersionUID = 1L;

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
    // TODO: implement proper validation
    System.out.println();
  }
}
