package org.keycloak.broker.saml.aggregate.metadata;

import org.keycloak.Config.Scope;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class InMemorySAMLAggregateMetadataProviderFactory
    implements SAMLAggregateMetadataProviderFactory {
  
  public static final String ID = "in-memory-saml-md";

  @Override
  public SAMLAggregateMetadataProvider create(KeycloakSession session) {
    return new InMemorySAMLAggregateMetadataProvider(session);
  }

  @Override
  public void init(Scope config) {
    System.out.println();
  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
    System.out.println();
  }

  @Override
  public void close() {
    System.out.println();
  }

  @Override
  public String getId() {
    return ID;
  }

}
