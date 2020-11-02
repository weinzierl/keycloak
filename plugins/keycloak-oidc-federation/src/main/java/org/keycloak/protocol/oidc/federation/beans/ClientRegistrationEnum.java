package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public enum ClientRegistrationEnum {

    @JsonProperty("automatic")
    AUTOMATIC,
    @JsonProperty("explicit")
    EXPLICIT
}
