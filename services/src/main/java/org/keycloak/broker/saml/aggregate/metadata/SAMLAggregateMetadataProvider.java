package org.keycloak.broker.saml.aggregate.metadata;

import java.util.List;
import java.util.Optional;

import org.keycloak.broker.saml.aggregate.SAMLAggregateIdentityProviderConfig;
import org.keycloak.provider.Provider;

public interface SAMLAggregateMetadataProvider extends Provider {

  void parseMetadata(SAMLAggregateIdentityProviderConfig providerConfig);

  Optional<SAMLIdpDescriptor> lookupIdpByEntityId(
      SAMLAggregateIdentityProviderConfig providerConfig, String entityId);

  List<SAMLIdpDescriptor> lookupEntities(SAMLAggregateIdentityProviderConfig providerConfig,
      String matchString);
}
