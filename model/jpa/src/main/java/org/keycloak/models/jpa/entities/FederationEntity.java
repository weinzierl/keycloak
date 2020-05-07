package org.keycloak.models.jpa.entities;

import java.util.HashSet;
import java.util.Set;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;

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

	@Column(name = "UPDATE_EVERY_MINUTES")
	private Integer updateEveryMinutes;

	@Column(name = "TOTAL_IDPS")
	private Integer totalIdps;

	@Column(name = "ADDED_IDPS")
	private Integer addedIdps;

	@Column(name = "CREATED")
	private Long created;

	@Column(name = "LAST_UPDATED")
	private Long lastUpdated;
	
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "REALM_ID")
    protected RealmEntity realm;
	
	@ElementCollection
    @Column(name="VALUE", columnDefinition = "TEXT")
    @CollectionTable(name="FEDERATION_SKIP_ENTITIES", joinColumns={ @JoinColumn(name="FEDERATION_ID") })
    private Set<String> skipEntities = new HashSet<String>();
	
	@ManyToMany(mappedBy = "federations")
	private Set<IdentityProviderEntity> identityproviders = new HashSet<IdentityProviderEntity>();

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
	
	public RealmEntity getRealm() {
		return realm;
	}

	public void setRealm(RealmEntity realm) {
		this.realm = realm;
	}

	public Integer getUpdateEveryMinutes() {
		return updateEveryMinutes;
	}

	public void setUpdateEveryMinutes(Integer updateEveryMinutes) {
		this.updateEveryMinutes = updateEveryMinutes;
	}

	public Integer getTotalIdps() {
		return totalIdps;
	}

	public void setTotalIdps(Integer totalIdps) {
		this.totalIdps = totalIdps;
	}

	public Integer getAddedIdps() {
		return addedIdps;
	}

	public void setAddedIdps(Integer addedIdps) {
		this.addedIdps = addedIdps;
	}

	public Long getCreated() {
		return created;
	}

	public void setCreated(Long created) {
		this.created = created;
	}

	public Long getLastUpdated() {
		return lastUpdated;
	}

	public void setLastUpdated(Long lastUpdated) {
		this.lastUpdated = lastUpdated;
	}

	public Set<String> getSkipEntities() {
		return skipEntities;
	}

	public void setSkipEntities(Set<String> skipEntities) {
		this.skipEntities = skipEntities;
	}

	public Set<IdentityProviderEntity> getIdentityproviders() {
		return identityproviders;
	}

	public void setIdentityproviders(Set<IdentityProviderEntity> identityproviders) {
		this.identityproviders = identityproviders;
	}
	
	public String getProviderId() {
		return providerId;
	}

	public void setProviderId(String providerId) {
		this.providerId = providerId;
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
