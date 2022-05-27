package org.keycloak.models.jpa.entities;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CascadeType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MapKeyColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.OneToMany;
import javax.persistence.Table;

import org.hibernate.annotations.BatchSize;
import org.keycloak.models.jpa.converter.ListJsonConverter;

@Entity
@Table(name = "FEDERATION")
@NamedQueries({
    @NamedQuery(name="findFederationByAliasAndRealm", query="select f from FederationEntity f where f.alias = :alias and f.realm.id = :realmId")
})
public class FederationEntity {

	@Id
	@Column(name = "INTERNAL_ID", length = 36)
	@Access(AccessType.PROPERTY) // we do this because relationships often fetch id, but not entity. This avoids an extra SQL
	protected String internalId;

	@Column(name = "URL")
	private String url;
	
	@Column(name = "ALIAS")
	private String alias;
	
	@Column(name = "DISPLAY_NAME")
	private String displayName;

	@Column(name = "PROVIDER_ID")
	private String providerId;

	//values= ALL,IDPS,CLIENTS
	@Column(name = "CATEGORY")
	private String category;

	@Column(name = "UPDATE_FREQUENCY_IN_MINS")
	private Integer updateFrequencyInMins;

	@Column(name = "VALID_UNTIL_TIMESTAMP")
	private Long validUntilTimestamp;

	@Column(name = "LAST_METADATA_REFRESH_TIMESTAMP")
	private Long lastMetadataRefreshTimestamp;

	@BatchSize(size = 50)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;

	@BatchSize(size = 50)
    @ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="FEDERATION_CONFIG", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Map<String, String> config;

	@BatchSize(size = 50)
	@ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITYID_DENYLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> entityIdDenyList = new HashSet<String>();

	@BatchSize(size = 50)
	@ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITYID_ALLOWLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> entityIdAllowList = new HashSet<String>();

	@BatchSize(size = 50)
	@ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="REGISTRATION_AUTHORITY_DENYLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> registrationAuthorityDenyList = new HashSet<String>();

	@BatchSize(size = 50)
    @ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="REGISTRATION_AUTHORITY_ALLOWLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> registrationAuthorityAllowList = new HashSet<String>();

	@BatchSize(size = 50)
	@ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITY_CATEGORY_DENYLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
	@Convert(converter = ListJsonConverter.class, attributeName = "value")
    private Map<String, List<String>> categoryDenyList;

	@BatchSize(size = 50)
	@ElementCollection
    @MapKeyColumn(name="NAME")
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="ENTITY_CATEGORY_ALLOWLIST", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    @Convert(converter = ListJsonConverter.class, attributeName = "value")
    private Map<String, List<String>> categoryAllowList;

	@BatchSize(size = 50)
	@OneToMany(cascade =CascadeType.REMOVE,  mappedBy = "federation")
	private List<FederationMapperEntity> federationMapperEntities = new ArrayList<FederationMapperEntity>();

	public String getInternalId() {
		return internalId;
	}

	public void setInternalId(String internalId) {
		this.internalId = internalId;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}	
	
	public String getAlias() {
		return alias;
	}

	public void setAlias(String alias) {
		this.alias = alias;
	}
	
	public String getDisplayName() {
		return displayName;
	}

	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	public String getCategory() {
		return category;
	}

	public void setCategory(String category) {
		this.category = category;
	}

	public RealmEntity getRealm() {
		return realm;
	}

	public void setRealm(RealmEntity realm) {
		this.realm = realm;
	}

	public Map<String, String> getConfig() {
		return config;
	}

	public void setConfig(Map<String, String> config) {
		this.config = config;
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

	public void setLastMetadataRefreshTimestamp(Long lastMetadataRefreshTimestamp ) {
		this.lastMetadataRefreshTimestamp = lastMetadataRefreshTimestamp ;
	}

	public Set<String> getEntityIdDenyList() {
		return entityIdDenyList;
	}

	public void setEntityIdDenyList(Set<String> entityIdDenyList) {
		this.entityIdDenyList = entityIdDenyList;
	}

	public Set<String> getEntityIdAllowList() {
        return entityIdAllowList;
    }

    public void setEntityIdAllowList(Set<String> entityIdAllowList) {
        this.entityIdAllowList = entityIdAllowList;
    }

    public Set<String> getRegistrationAuthorityDenyList() {
        return registrationAuthorityDenyList;
    }

    public void setRegistrationAuthorityDenyList(Set<String> registrationAuthorityDenyList) {
        this.registrationAuthorityDenyList = registrationAuthorityDenyList;
    }

    public Set<String> getRegistrationAuthorityAllowList() {
        return registrationAuthorityAllowList;
    }

    public void setRegistrationAuthorityAllowList(Set<String> registrationAuthorityAllowList) {
        this.registrationAuthorityAllowList = registrationAuthorityAllowList;
    }

	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
	}

	public Long getValidUntilTimestamp() {
		return validUntilTimestamp;
	}

	public void setValidUntilTimestamp(Long validUntilTimestamp) {
		this.validUntilTimestamp = validUntilTimestamp;
	}

	public Map<String, List<String>> getCategoryDenyList() {
        return categoryDenyList;
    }

    public void setCategoryDenyList(Map<String, List<String>> categoryDenyList) {
        this.categoryDenyList = categoryDenyList;
    }
    
    public Map<String, List<String>> getCategoryAllowList() {
        return categoryAllowList;
    }

    public void setCategoryAllowList(Map<String, List<String>> categoryAllowList) {
        this.categoryAllowList = categoryAllowList;
    }

    public List<FederationMapperEntity> getFederationMapperEntities() {
        return federationMapperEntities;
    }

    public void setFederationMapperEntities(List<FederationMapperEntity> federationMapperEntities) {
        this.federationMapperEntities = federationMapperEntities;
    }

    @Override
	public boolean equals(Object o) {
		if (this == o)
			return true;
		if (o == null)
			return false;
		if (!(o instanceof FederationEntity))
			return false;

		FederationEntity that = (FederationEntity) o;

		if (!internalId.equals(that.internalId))
			return false;

		return true;
	}

	@Override
	public int hashCode() {
		return internalId.hashCode();
	}

}
