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
package org.keycloak.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * <p>A model type for the identity provider federations.</p>
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class IdentityProvidersFederationModel implements Serializable {

    private String internalId;

    private String alias;
 
    private String providerId;
    
    private String url;
    
    private Integer updateFrequencyInMins;
    
    private String displayName;
    
    private Long validUntilTimestamp;
    
    private Long lastMetadataRefreshTimestamp;

    private Set<String> skipIdps;
    
    private Set<String> identityprovidersAlias;
    
    private Map<String, String> config;
    

    public IdentityProvidersFederationModel() {
		super();
		this.skipIdps = new HashSet<String>();
		this.identityprovidersAlias = new HashSet<String>();
	}

    public IdentityProvidersFederationModel(IdentityProvidersFederationModel model) {
		super();
		this.setInternalId(model.getInternalId());
		this.setAlias(model.getAlias());
		this.setProviderId(model.getProviderId());
		this.setUrl(model.getUrl());
		this.setUpdateFrequencyInMins(model.getUpdateFrequencyInMins());
		this.setDisplayName(model.getDisplayName());
		this.setSkipIdps(model.getSkipIdps() != null ? model.getSkipIdps() : new HashSet<String>());
		this.setLastMetadataRefreshTimestamp(model.getLastMetadataRefreshTimestamp());
		this.setIdentityprovidersAlias(model.getIdentityprovidersAlias());
		this.setConfig(model.getConfig());
	}

	public IdentityProvidersFederationModel(String internalId, String alias, String providerId, String url,
			Integer refreshEveryMinutes, String displayName, Long lastMetadataRefreshTimestamp,Set<String> skipIdps,Set<String> identityprovidersAlias) {
		super();
		this.internalId = internalId;
		this.alias = alias;
		this.providerId = providerId;
		this.url = url;
		this.updateFrequencyInMins = refreshEveryMinutes;
		this.displayName = displayName;
		this.skipIdps = (skipIdps != null) ? skipIdps : new HashSet<String>();
		this.lastMetadataRefreshTimestamp = lastMetadataRefreshTimestamp;
		this.identityprovidersAlias = identityprovidersAlias;
	}



	public String getInternalId() {
        return this.internalId;
    }

    public void setInternalId(String internalId) {
        this.internalId = internalId;
    }

    public String getAlias() {
        return this.alias;
    }

    public void setAlias(String id) {
        this.alias = id;
    }

    public String getProviderId() {
        return this.providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }
    

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public Integer getUpdateFrequencyInMins() {
		return updateFrequencyInMins;
	}

	public void setUpdateFrequencyInMins(Integer updateFrequencyInMins) {
		this.updateFrequencyInMins = updateFrequencyInMins;
	}

	public Long getLastMetadataRefreshTimestamp() {
		return lastMetadataRefreshTimestamp;
	}

	public void setLastMetadataRefreshTimestamp(Long lastMetadataRefreshTimestamp) {
		this.lastMetadataRefreshTimestamp = lastMetadataRefreshTimestamp;
	}

	public Set<String> getSkipIdps() {
		return skipIdps;
	}

	public void setSkipIdps(Set<String> skipIdps) {
		this.skipIdps = skipIdps;
	}

	public Set<String> getIdentityprovidersAlias() {
		return identityprovidersAlias;
	}

	public void setIdentityprovidersAlias(Set<String> identityprovidersAlias) {
		this.identityprovidersAlias = identityprovidersAlias;
	}

	public Long getValidUntilTimestamp() {
		return validUntilTimestamp;
	}

	public void setValidUntilTimestamp(Long validUntilTimestamp) {
		this.validUntilTimestamp = validUntilTimestamp;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
	}
	
}
