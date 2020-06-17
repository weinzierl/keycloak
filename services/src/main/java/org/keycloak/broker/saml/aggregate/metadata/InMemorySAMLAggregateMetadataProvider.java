package org.keycloak.broker.saml.aggregate.metadata;

import static java.util.Collections.emptyList;
import static java.util.Objects.isNull;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.jboss.logging.Logger;
import org.keycloak.broker.saml.aggregate.SAMLAggregateIdentityProviderConfig;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.models.KeycloakSession;
import org.keycloak.saml.common.exceptions.ParsingException;

import com.google.common.collect.Maps;

public class InMemorySAMLAggregateMetadataProvider implements SAMLAggregateMetadataProvider {

  private static final Logger LOG = Logger.getLogger(InMemorySAMLAggregateMetadataProvider.class);

  private final KeycloakSession session;

  private final Map<String, Map<String, SAMLIdpDescriptor>> metadataStore = Maps.newConcurrentMap();

  private final SAMLAggregateParser parser = new SAMLAggregateParser();

  public InMemorySAMLAggregateMetadataProvider(KeycloakSession session) {
    this.session = session;
  }

  @Override
  public void close() {
    metadataStore.clear();
  }

  protected InputStream fetchMetadata(SAMLAggregateIdentityProviderConfig providerConfig) {
    final String url = providerConfig.getMetadataUrl();

    LOG.infov("Parsing metadata from URL: {0} for provider {1}", url, providerConfig.getAlias());

    try {
      return session.getProvider(HttpClientProvider.class).get(url);
    } catch (IOException e) {
      final String errorMsg = String.format("Error parsing metadata for provider %s from %s: %s",
          providerConfig.getDisplayName(), url, e.getMessage());
      throw new SAMLMetadataParsingError(errorMsg, e);
    }
  }

  @Override
  public void parseMetadata(SAMLAggregateIdentityProviderConfig providerConfig) {
    final String url = providerConfig.getMetadataUrl();

    InputStream mdStream = fetchMetadata(providerConfig);

    try {
      Map<String, SAMLIdpDescriptor> mdMap = parser.parseMetadata(mdStream);

      LOG.infov("Parsed {0} entities for provider {1}", mdMap.keySet().size(),
          providerConfig.getAlias());

      metadataStore.put(providerConfig.getInternalId(), mdMap);

    } catch (ParsingException e) {

      final String errorMsg =
          String.format("Error parsing metadata from %s: %s", url, e.getMessage());
      LOG.error(errorMsg, e);
      throw new SAMLMetadataParsingError(errorMsg, e);

    }
  }

  @Override
  public Optional<SAMLIdpDescriptor> lookupIdpByEntityId(
      SAMLAggregateIdentityProviderConfig providerConfig, String entityId) {

    if (isNull(metadataStore.get(providerConfig.getInternalId()))) {
      return Optional.empty();
    }

    return Optional.ofNullable(metadataStore.get(providerConfig.getInternalId()).get(entityId));
  }

  @Override
  public List<SAMLIdpDescriptor> lookupEntities(SAMLAggregateIdentityProviderConfig providerConfig,
      String matchString) {

    if (isNull(metadataStore.get(providerConfig.getInternalId()))) {
      return emptyList();
    }

    if (matchString.length() < 3) {
      return emptyList();
    }
    
    // FIXME: tbd
    return null;
  }



}
