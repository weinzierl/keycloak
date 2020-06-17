package org.keycloak.broker.saml.aggregate.metadata;

import java.util.Map;
import java.util.Optional;

public class SAMLIdpUIInfo {

  private Map<String, String> localizedDisplayNames;

  public SAMLIdpUIInfo() {

  }


  public Optional<String> getDisplayName(String locale) {
    return null;
  }

  public Map<String, String> getLocalizedDisplayNames() {
    return localizedDisplayNames;
  }

}
