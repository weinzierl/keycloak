package org.keycloak.broker.saml.aggregate.metadata;

import static java.util.Objects.isNull;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.util.DocumentUtil;
import org.w3c.dom.Element;

@JsonDeserialize(builder = SAMLIdpDescriptor.Builder.class)
public class SAMLIdpDescriptor implements Serializable {

  private static final long serialVersionUID = 1L;

  private final String entityId;
  private final String displayName;

  private final boolean postBindingResponse;
  private final boolean postBindingLogout;

  private final String singleLogoutServiceUrl;
  private final String singleSignOnServiceUrl;

  private final String encryptionKey;
  private final String signingCertificate;

  private final IDPSSODescriptorType descriptor;

  private SAMLIdpDescriptor(Builder builder) {
    this.entityId = builder.entityId;
    this.displayName = builder.displayName;
    this.postBindingLogout = builder.postBindingLogout;
    this.postBindingResponse = builder.postBindingResponse;
    this.singleSignOnServiceUrl = builder.singleSignOnServiceUrl;
    this.singleLogoutServiceUrl = builder.singleLogoutServiceUrl;
    this.descriptor = builder.descriptor;
    this.encryptionKey = builder.encryptionKey;
    this.signingCertificate = builder.signingCertificate;
  }

  public String getEntityId() {
    return entityId;
  }

  public String getDisplayName() {
    return displayName;
  }

  public boolean isPostBindingResponse() {
    return postBindingResponse;
  }

  public boolean isPostBindingLogout() {
    return postBindingLogout;
  }

  public String getSingleLogoutServiceUrl() {
    return singleLogoutServiceUrl;
  }

  public String getSingleSignOnServiceUrl() {
    return singleSignOnServiceUrl;
  }

  public String getEncryptionKey() {
    return encryptionKey;
  }

  public String getSigningCertificate() {
    return signingCertificate;
  }

  public IDPSSODescriptorType getDescriptor() {
    return descriptor;
  }

  public static SAMLIdpDescriptor buildFor(String entityId, IDPSSODescriptorType descriptor) {
    return new Builder(entityId, descriptor).build();
  }

  @JsonPOJOBuilder
  public static class Builder {
    private String entityId;

    private String displayName;

    private boolean postBindingResponse;
    private boolean postBindingLogout;

    private String singleLogoutServiceUrl;
    private String singleSignOnServiceUrl;

    private String encryptionKey;
    private String signingCertificate;

    private IDPSSODescriptorType descriptor;

    private void initSso() {
      Optional<String> ssoPbUrl = descriptor.getSingleSignOnService()
        .stream()
        .filter(e -> e.getBinding()
          .toString()
          .equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get()))
        .map(EndpointType::getLocation)
        .map(URI::toString)
        .findFirst();

      Optional<String> ssoRedirectUrl = descriptor.getSingleSignOnService()
        .stream()
        .filter(e -> e.getBinding()
          .toString()
          .equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get()))
        .map(EndpointType::getLocation)
        .map(URI::toString)
        .findFirst();

      if (ssoPbUrl.isPresent()) {
        withSingleSignOnServiceUrl(ssoPbUrl.get());
        withPostBindingResponse(true);
      } else if (ssoRedirectUrl.isPresent()) {
        withSingleSignOnServiceUrl(ssoRedirectUrl.get());
        withPostBindingResponse(false);
      } else {
        throw new RuntimeException("No SSO usable binding found for: " + entityId);
      }

      Optional<String> sloPbUrl = descriptor.getSingleLogoutService()
        .stream()
        .filter(e -> e.getBinding()
          .toString()
          .equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get()))
        .map(EndpointType::getLocation)
        .map(URI::toString)
        .findFirst();

      Optional<String> sloRedirectUrl = descriptor.getSingleLogoutService()
        .stream()
        .filter(e -> e.getBinding()
          .toString()
          .equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get()))
        .map(EndpointType::getLocation)
        .map(URI::toString)
        .findFirst();

      if (sloPbUrl.isPresent()) {
        withSingleLogoutServiceUrl(sloPbUrl.get());
        withPostBindingLogout(true);
      } else if (sloRedirectUrl.isPresent()) {
        withSingleLogoutServiceUrl(sloRedirectUrl.get());
        withPostBindingLogout(false);
      }
    }

    private void initCerts() {
      List<KeyDescriptorType> keyDescriptor = descriptor.getKeyDescriptor();

      String defaultCertificate = null;

      if (keyDescriptor != null) {
        for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
          Element keyInfo = keyDescriptorType.getKeyInfo();
          Element x509KeyInfo =
              DocumentUtil.getChildElement(keyInfo, new QName("dsig", "X509Certificate"));
          if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
            withSigningCertificate(x509KeyInfo.getTextContent());
          } else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
            withEncryptionKey(x509KeyInfo.getTextContent());
          } else if (keyDescriptorType.getUse() == null) {
            defaultCertificate = x509KeyInfo.getTextContent();
          }
        }

        if (!isNull(defaultCertificate)) {
          if (isNull(signingCertificate)) {
            withSigningCertificate(defaultCertificate);
          }

          if (isNull(encryptionKey)) {
            withEncryptionKey(defaultCertificate);
          }
        }
      }
    }

    private void initDisplayName() {
      if (!isNull(descriptor.getExtensions())) {
        if (!isNull(descriptor.getExtensions().getUIInfo())) {
          withDisplayName(descriptor.getExtensions()
            .getUIInfo()
            .getDisplayName()
            .stream()
            .filter(l -> l.getLang().equals("en"))
            .map(LocalizedNameType::getValue)
            .findFirst()
            .orElse(null));
        } else {
          System.out.println("null ui info");
        }
      } else {
        System.out.println("null extensions");
      }
    }

    public Builder withEntityId(String entityId) {
      this.entityId = entityId;
      return this;
    }
    public Builder withDescriptor(IDPSSODescriptorType descriptor) {
      this.descriptor = descriptor;
      return this;
    }
    public Builder withDisplayName(String displayName) {
      this.displayName = displayName;
      return this;
    }
    public Builder withPostBindingResponse(Boolean postBindingResponse) {
      this.postBindingResponse = postBindingResponse;
      return this;
    }
    public Builder withPostBindingLogout(Boolean postBindingLogout) {
      this.postBindingLogout = postBindingLogout;
      return this;
    }
    public Builder withSingleLogoutServiceUrl(String singleLogoutServiceUrl) {
      this.singleLogoutServiceUrl = singleLogoutServiceUrl;
      return this;
    }
    public Builder withSingleSignOnServiceUrl(String singleSignOnServiceUrl) {
      this.singleSignOnServiceUrl = singleSignOnServiceUrl;
      return this;
    }
    public Builder withSigningCertificate(String signingCertificate) {
      this.signingCertificate = signingCertificate;
      return this;
    }
    public Builder withEncryptionKey(String encryptionKey) {
      this.encryptionKey = encryptionKey;
      return this;
    }
    public Builder() {

    }

    public Builder(String entityId, IDPSSODescriptorType descriptor) {
      withEntityId(entityId);
      withDescriptor(descriptor);
      initSso();
      initCerts();
      initDisplayName();
    }

    public SAMLIdpDescriptor build() {
      return new SAMLIdpDescriptor(this);
    }
  }

}
