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
package org.keycloak.broker.federation;

import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderFactory;

import java.io.InputStream;
import java.util.Map;
import java.util.Set;


public interface IdpFederationProviderFactory extends ProviderFactory<IdpFederationProvider> {

	
    String getName();

    
    IdpFederationProvider create(KeycloakSession session, IdentityProvidersFederationModel model, String realmId);


	static IdpFederationProviderFactory getIdpFederationProviderFactoryById(KeycloakSession session, String providerId) {
		return (IdpFederationProviderFactory) session.getKeycloakSessionFactory()
				.getProviderFactories(IdpFederationProvider.class).stream().filter(pf -> pf.getId().equals(providerId))
				.findAny().orElse(null);
	};
    
}