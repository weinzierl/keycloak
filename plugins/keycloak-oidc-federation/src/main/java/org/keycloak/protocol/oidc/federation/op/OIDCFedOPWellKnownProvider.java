package org.keycloak.protocol.oidc.federation.op;
/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */



import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.keycloak.common.util.Time;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.OIDCWellKnownProvider;
import org.keycloak.protocol.oidc.federation.beans.EntityStatement;
import org.keycloak.protocol.oidc.federation.beans.Metadata;
import org.keycloak.protocol.oidc.federation.beans.OPMetadata;
import org.keycloak.protocol.oidc.federation.exceptions.InternalServerErrorException;
import org.keycloak.protocol.oidc.federation.helpers.FedUtils;
import org.keycloak.protocol.oidc.federation.model.OIDCFedConfigEntity;
import org.keycloak.protocol.oidc.federation.model.OIDCFedConfigService;
import org.keycloak.protocol.oidc.federation.rest.OIDCFederationResourceProvider;
import org.keycloak.protocol.oidc.federation.rest.OIDCFederationResourceProviderFactory;
import org.keycloak.protocol.oidc.federation.rest.op.FederationOPService;
import org.keycloak.protocol.oidc.representations.OIDCConfigurationRepresentation;
import org.keycloak.services.Urls;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;
import org.keycloak.util.JsonSerialization;


public class OIDCFedOPWellKnownProvider extends OIDCWellKnownProvider {

    public static final List<String> CLIENT_REGISTRATION_TYPES_SUPPORTED = Arrays.asList("automatic", "explicit");

    private KeycloakSession session;
    private OIDCFedConfigService configurationService;

    public OIDCFedOPWellKnownProvider(KeycloakSession session) {
        super(session);
        this.session = session;
        this.configurationService = new OIDCFedConfigService(session);
    }

    @Override
    public Object getConfig() {
        
        OIDCFedConfigEntity conf =configurationService.getEntity();
        //realm without authority hints must not expose this web service
        if (conf == null || conf.getConfiguration() == null || conf.getConfiguration().getAuthorityHints().isEmpty())
            throw new NotFoundException("This realm is not a OIDC Federation member");
        
        UriInfo frontendUriInfo = session.getContext().getUri(UrlType.FRONTEND);
        UriInfo backendUriInfo = session.getContext().getUri(UrlType.BACKEND);

        RealmModel realm = session.getContext().getRealm();

        UriBuilder frontendUriBuilder = RealmsResource.realmBaseUrl(frontendUriInfo);
        UriBuilder backendUriBuilder = RealmsResource.realmBaseUrl(backendUriInfo);

        OPMetadata config;
        try {
            config = from(((OIDCConfigurationRepresentation) super.getConfig()));
        } catch (IOException e) {
            throw new InternalServerErrorException("Could not form the configuration response");
        }

        //additional federation-specific configuration
        config.setFederationRegistrationEndpoint(backendUriBuilder.clone().path(OIDCFederationResourceProviderFactory.ID).path(OIDCFederationResourceProvider.class, "getFederationOPService").path(FederationOPService.class, "getFederationRegistration").build(realm.getName()).toString());
        config.setPushedAuthorizationRequestEndpoint(backendUriBuilder.clone().path(OIDCFederationResourceProviderFactory.ID).path(OIDCFederationResourceProvider.class, "getFederationOPService").path(FederationOPService.class, "postPushedAuthorization").build(realm.getName()).toString());
        config.setClientRegistrationTypesSupported("both".equals(conf.getConfiguration().getRegistrationType()) ? CLIENT_REGISTRATION_TYPES_SUPPORTED : Arrays.asList(conf.getConfiguration().getRegistrationType()));
//        config.setClientRegistrationAuthnMethodsSupported(clientRegistrationAuthnMethodsSupported);

        Metadata metadata = new Metadata();
        metadata.setOp(config);

        EntityStatement entityStatement = new EntityStatement();
        entityStatement.setMetadata(metadata);
        entityStatement.setAuthorityHints(conf.getConfiguration().getAuthorityHints().stream().collect(Collectors.toList()));
        entityStatement.setJwks(FedUtils.getKeySet(session));
        entityStatement.issuer(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));
        entityStatement.subject(Urls.realmIssuer(frontendUriInfo.getBaseUri(), realm.getName()));
        entityStatement.issuedNow();
        entityStatement.exp(Long.valueOf(Time.currentTime()) + Long.valueOf(conf.getConfiguration().getExpirationTime()));

        //sign and encode entity statement
        String encodedToken = session.tokens().encode(entityStatement);

        return encodedToken;
    }

    @Override
    public void close() {
    }

    public static OPMetadata from(OIDCConfigurationRepresentation representation) throws IOException {
        return JsonSerialization.readValue(JsonSerialization.writeValueAsString(representation), OPMetadata.class);
    }

}
