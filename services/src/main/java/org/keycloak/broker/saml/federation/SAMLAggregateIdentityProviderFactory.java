package org.keycloak.broker.saml.federation;

import org.keycloak.broker.provider.AbstractIdentityProviderFactory;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;

public class SAMLAggregateIdentityProviderFactory
    extends AbstractIdentityProviderFactory<SAMLAggregateIdentityProvider> {

  public static final String PROVIDER_ID = "saml-aggregate";
  public static final String PROVIDER_NAME = "SAML v2.0 Aggregate";

  public SAMLAggregateIdentityProviderFactory() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public String getName() {
    return PROVIDER_NAME;
  }



  @SuppressWarnings("unchecked")
  @Override
  public SAMLAggregateIdentityProviderConfig createConfig() {
    return new SAMLAggregateIdentityProviderConfig();
  }

  @Override
  public String getId() {
    return PROVIDER_ID;
  }

  @Override
  public SAMLAggregateIdentityProvider create(KeycloakSession session,
      IdentityProviderModel model) {
    return new SAMLAggregateIdentityProvider(session,
        new SAMLAggregateIdentityProviderConfig(model));
  }

}
