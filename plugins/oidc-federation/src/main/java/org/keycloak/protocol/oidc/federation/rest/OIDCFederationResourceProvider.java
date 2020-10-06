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

package org.keycloak.protocol.oidc.federation.rest;

import javax.ws.rs.Path;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.protocol.oidc.federation.rest.op.FederationOPService;
import org.keycloak.services.resource.RealmResourceProvider;

public class OIDCFederationResourceProvider implements RealmResourceProvider {

    private KeycloakSession session;

    public OIDCFederationResourceProvider(KeycloakSession session) {
        this.session = session;
    }

    @Override
    public Object getResource() {
        return this;
    }

    @Override
    public void close() {
    }
    
    @Path("op")
    public FederationOPService getFederationOPService() {
        FederationOPService opService = new FederationOPService(session);
        ResteasyProviderFactory.getInstance().injectProperties(opService);
        return opService;
    }
    
    
    
}
