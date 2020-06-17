package org.keycloak.broker.saml.aggregate.metadata;

import org.keycloak.Config.Scope;
import org.keycloak.events.EventListenerProvider;
import org.keycloak.events.EventListenerProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;

public class SAMLAggregateIdentityProviderEventListenerFactory
    implements EventListenerProviderFactory {

  public static final String ID = "saml-aggregate-idp-event-listener";
  
  public SAMLAggregateIdentityProviderEventListenerFactory() {
    // TODO Auto-generated constructor stub
  }

  @Override
  public EventListenerProvider create(KeycloakSession session) {
    return null;
  }

  @Override
  public void init(Scope config) {
    // TODO Auto-generated method stub

  }

  @Override
  public void postInit(KeycloakSessionFactory factory) {
  }

  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  @Override
  public String getId() {
    return ID;
  }

}
