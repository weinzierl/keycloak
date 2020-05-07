package org.keycloak.representations.idm;

import java.util.HashSet;
import java.util.Set;

public class IdentityProvidersFederationRepresentation {
	

    private String internalId;
 
    private String providerId;

    private String alias;
    
    private String url;
    
    private String realmId;
    
    private Integer refreshEveryMinutes;
    
    private String displayName;
    
    private Long lastUpdated;

    private Set<String> skipIdps;
    
    private Set<String> identityprovidersAlias;
    
    private Integer totalIdps;
    
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

	public String getRealmId() {
		return realmId;
	}

	public void setRealmId(String realmId) {
		this.realmId = realmId;
	}

	public Integer getRefreshEveryMinutes() {
		return refreshEveryMinutes;
	}

	public void setRefreshEveryMinutes(Integer refreshEveryMinutes) {
		this.refreshEveryMinutes = refreshEveryMinutes;
	}

	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public Long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Long lastUpdated) {
		this.lastUpdated = lastUpdated;
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

	public Integer getTotalIdps() {
		return totalIdps;
	}

	public void setTotalIdps(Integer totalIdps) {
		this.totalIdps = totalIdps;
	}

    

}
