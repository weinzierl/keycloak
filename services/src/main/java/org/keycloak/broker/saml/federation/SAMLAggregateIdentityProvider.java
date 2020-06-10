package org.keycloak.broker.saml.federation;

import javax.ws.rs.core.Response;

import org.keycloak.broker.provider.AbstractIdentityProvider;
import org.keycloak.models.FederatedIdentityModel;
import org.keycloak.models.KeycloakSession;

public class SAMLAggregateIdentityProvider extends AbstractIdentityProvider<SAMLAggregateIdentityProviderConfig>{

  public SAMLAggregateIdentityProvider(KeycloakSession session,
      SAMLAggregateIdentityProviderConfig config) {
    super(session, config);
  }

  @Override
  public Response retrieveToken(KeycloakSession session, FederatedIdentityModel identity) {
    
    return null;
  }
}