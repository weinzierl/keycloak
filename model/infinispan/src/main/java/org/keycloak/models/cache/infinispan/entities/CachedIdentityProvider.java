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

package org.keycloak.models.cache.infinispan.entities;


import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;


public class CachedIdentityProvider extends AbstractRevisioned implements InRealm {

	
	private String realmId;
	
    private IdentityProviderModel model;
    
    
	public CachedIdentityProvider(Long revision, RealmModel realm, IdentityProviderModel model) {
		super(revision, model.getInternalId());
		this.realmId = realm.getId();
		this.model = model;
	}


	@Override
	public String getRealm() {
		return realmId;
	}


	public IdentityProviderModel getModel() {
		return model;
	}
	

}
