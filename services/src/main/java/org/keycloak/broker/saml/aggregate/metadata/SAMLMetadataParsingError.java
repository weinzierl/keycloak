package org.keycloak.broker.saml.aggregate.metadata;

public class SAMLMetadataParsingError extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SAMLMetadataParsingError(String message) {
    super(message);
  }

  public SAMLMetadataParsingError(String message, Throwable cause) {
    super(message, cause);
  }

}
