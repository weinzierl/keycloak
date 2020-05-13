package org.keycloak.representations.idm;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class IdentityProvidersFederationRepresentation {
	

    private String internalId;
 
    private String providerId;

    private String alias;
    
    private String url;
    
    private Integer updateFrequencyInMins;
    
    private String displayName;
    
    private Long validUntilTimestamp;
    
    private Long lastMetadataRefreshTimestamp;

    private Set<String> skipIdps;
    
    private Set<String> identityprovidersAlias;
    
    private Map<String, String> config;
    
    public IdentityProvidersFederationRepresentation() {
    	this.skipIdps = new HashSet<String>();
    	this.identityprovidersAlias = new HashSet<String>();
    }

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
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

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
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
