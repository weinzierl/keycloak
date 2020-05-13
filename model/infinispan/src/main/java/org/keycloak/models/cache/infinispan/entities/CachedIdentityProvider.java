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
	private String internalId;
    private String alias;
    private String providerId;
    private boolean enabled;
    private boolean trustEmail;
    private boolean storeToken;
    protected boolean addReadTokenRoleOnCreate;
    protected boolean linkOnly;
    private boolean authenticateByDefault;
    private String firstBrokerLoginFlowId;
    private String postBrokerLoginFlowId;
    private String displayName;
    private Map<String, String> config = new HashMap<>();
    private Set<String> federations = new HashSet<>();
    
    
    
	public CachedIdentityProvider(Long revision, RealmModel realm, IdentityProviderModel model) {
		super(revision, model.getInternalId());
		this.realmId = realm.getId();
		this.internalId = model.getInternalId();
		this.alias = model.getAlias();
		this.providerId = model.getProviderId();
		this.enabled = model.isEnabled();
		this.trustEmail = model.isTrustEmail();
		this.storeToken = model.isStoreToken();
		this.addReadTokenRoleOnCreate = model.isAddReadTokenRoleOnCreate();
		this.linkOnly = model.isLinkOnly();
		this.authenticateByDefault = model.isAuthenticateByDefault();
		this.firstBrokerLoginFlowId = model.getFirstBrokerLoginFlowId();
		this.postBrokerLoginFlowId = model.getPostBrokerLoginFlowId();
		this.displayName = model.getDisplayName();
		this.config = model.getConfig();
	}


	@Override
	public String getRealm() {
		return realmId;
	}

	public String getRealmId() {
		return realmId;
	}

	public String getInternalId() {
		return internalId;
	}

	public String getAlias() {
		return alias;
	}

	public String getProviderId() {
		return providerId;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public boolean isTrustEmail() {
		return trustEmail;
	}

	public boolean isStoreToken() {
		return storeToken;
	}

	public boolean isAddReadTokenRoleOnCreate() {
		return addReadTokenRoleOnCreate;
	}

	public boolean isLinkOnly() {
		return linkOnly;
	}

	public boolean isAuthenticateByDefault() {
		return authenticateByDefault;
	}

	public String getFirstBrokerLoginFlowId() {
		return firstBrokerLoginFlowId;
	}

	public String getPostBrokerLoginFlowId() {
		return postBrokerLoginFlowId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public Set<String> getFederations() {
		return federations;
	}
	
}
