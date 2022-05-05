package org.keycloak.protocol.oidc.mappers;

import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.UserModel;
import org.keycloak.representations.AccessToken;

public interface OIDCIntrospectionMapper {

    AccessToken transformAccessTokenForIntrospection(AccessToken token, ProtocolMapperModel mappingModel, UserModel user);
}
