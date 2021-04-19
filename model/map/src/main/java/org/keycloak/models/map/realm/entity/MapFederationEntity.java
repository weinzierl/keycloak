package org.keycloak.models.map.realm.entity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.models.FederationMapperModel;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapFederationEntity implements UpdatableEntity {

    private String internalId;
    private String alias;
    private String providerId;
    private String url;
    private Integer updateFrequencyInMins;
    private String displayName;
    private Long validUntilTimestamp;
    private Long lastMetadataRefreshTimestamp;
    private Map<String, String> config;
    private Set<String> entityIdDenyList = new HashSet<>();
    private Set<String> entityIdAllowList = new HashSet<>();
    private Set<String> registrationAuthorityDenyList = new HashSet<>();
    private Set<String> registrationAuthorityAllowList = new HashSet<>();
    private Map<String, List<String>> categoryDenyList = new HashMap<>();
    private Map<String, List<String>> categoryAllowList = new HashMap<>();
    private List<MapFederationMapperEntity> federationMappers = new ArrayList<>();
    private List<String> idps = new ArrayList<>();

    private boolean updated;

    private MapFederationEntity() {
    }

    public static MapFederationEntity fromModel(IdentityProvidersFederationModel model) {
        if (model == null) return null;
        MapFederationEntity entity = new MapFederationEntity();
        entity.setInternalId(model.getInternalId() == null ? KeycloakModelUtils.generateId() : model.getInternalId());
        entity.setAlias(model.getAlias());
        entity.setProviderId(model.getProviderId());
        entity.setDisplayName(model.getDisplayName());
        entity.setLastMetadataRefreshTimestamp(model.getLastMetadataRefreshTimestamp());
        entity.setUpdateFrequencyInMins(model.getUpdateFrequencyInMins());
        entity.setUrl(model.getUrl());
        entity.setValidUntilTimestamp(model.getValidUntilTimestamp());
        entity.setConfig(model.getConfig());
        entity.setEntityIdDenyList(model.getEntityIdDenyList());
        entity.setEntityIdAllowList(model.getEntityIdAllowList());
        entity.setCategoryDenyList(model.getCategoryDenyList());
        entity.setCategoryAllowList(model.getCategoryAllowList());
        entity.setRegistrationAuthorityDenyList(model.getRegistrationAuthorityDenyList());
        entity.setRegistrationAuthorityAllowList(model.getRegistrationAuthorityAllowList());
        entity.setFederationMappers(model.getFederationMapperModels().stream().map(MapFederationMapperEntity::fromModel).collect(Collectors.toList()));
        entity.setIdps(model.getIdps());

        return entity;
    }

    public static IdentityProvidersFederationModel toModel(MapFederationEntity entity) {
        if (entity == null) return null;
        IdentityProvidersFederationModel model = new IdentityProvidersFederationModel();

        model.setInternalId(entity.getInternalId());
        model.setAlias(entity.getAlias());
        model.setProviderId(entity.getProviderId());
        model.setDisplayName(entity.getDisplayName());
        model.setLastMetadataRefreshTimestamp(entity.getLastMetadataRefreshTimestamp());
        model.setUpdateFrequencyInMins(entity.getUpdateFrequencyInMins());
        model.setUrl(entity.getUrl());
        model.setValidUntilTimestamp(entity.getValidUntilTimestamp());
        model.setConfig(entity.getConfig());
        model.setEntityIdDenyList(entity.getEntityIdDenyList());
        model.setEntityIdAllowList(entity.getEntityIdAllowList());
        model.setCategoryDenyList(entity.getCategoryDenyList());
        model.setCategoryAllowList(entity.getCategoryAllowList());
        model.setRegistrationAuthorityDenyList(entity.getRegistrationAuthorityDenyList());
        model.setRegistrationAuthorityAllowList(entity.getRegistrationAuthorityAllowList());
        model.setFederationMapperModels(entity.getFederationMappers().stream().map(
            mapper -> {
               FederationMapperModel mapperModel = MapFederationMapperEntity.toModel(mapper);
               mapperModel.setFederationId(entity.getInternalId());
               return mapperModel;
            }).collect(Collectors.toList()));
        model.setIdps(entity.getIdps());

        return model;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.updated = !Objects.equals(this.internalId, internalId);
        this.internalId = internalId;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.updated = !Objects.equals(this.alias, alias);
        this.alias = alias;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.updated = !Objects.equals(this.providerId, providerId);
        this.providerId = providerId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.updated = !Objects.equals(this.url, url);
        this.url = url;
    }

    public Integer getUpdateFrequencyInMins() {
        return updateFrequencyInMins;
    }

    public void setUpdateFrequencyInMins(Integer updateFrequencyInMins) {
        this.updated = !Objects.equals(this.updateFrequencyInMins, updateFrequencyInMins);
        this.updateFrequencyInMins = updateFrequencyInMins;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.updated = !Objects.equals(this.displayName, displayName);
        this.displayName = displayName;
    }

    public Long getValidUntilTimestamp() {
        return validUntilTimestamp;
    }

    public void setValidUntilTimestamp(Long validUntilTimestamp) {
        this.updated = !Objects.equals(this.validUntilTimestamp, validUntilTimestamp);
        this.validUntilTimestamp = validUntilTimestamp;
    }

    public Long getLastMetadataRefreshTimestamp() {
        return lastMetadataRefreshTimestamp;
    }

    public void setLastMetadataRefreshTimestamp(Long lastMetadataRefreshTimestamp) {
        this.updated = !Objects.equals(this.lastMetadataRefreshTimestamp, lastMetadataRefreshTimestamp);
        this.lastMetadataRefreshTimestamp = lastMetadataRefreshTimestamp;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.updated = !Objects.equals(this.config, config);
        this.config = config;
    }

    public Set<String> getEntityIdDenyList() {
        return entityIdDenyList;
    }

    public void setEntityIdDenyList(Set<String> entityIdDenyList) {
        this.updated = !Objects.equals(this.entityIdDenyList, entityIdDenyList);
        this.entityIdDenyList = entityIdDenyList;
    }

    public Set<String> getEntityIdAllowList() {
        return entityIdAllowList;
    }

    public void setEntityIdAllowList(Set<String> entityIdAllowList) {
        this.updated = !Objects.equals(this.entityIdAllowList, entityIdAllowList);
        this.entityIdAllowList = entityIdAllowList;
    }

    public Set<String> getRegistrationAuthorityDenyList() {
        return registrationAuthorityDenyList;
    }

    public void setRegistrationAuthorityDenyList(Set<String> registrationAuthorityDenyList) {
        this.updated = !Objects.equals(this.registrationAuthorityDenyList, registrationAuthorityDenyList);
        this.registrationAuthorityDenyList = registrationAuthorityDenyList;
    }

    public Set<String> getRegistrationAuthorityAllowList() {
        return registrationAuthorityAllowList;
    }

    public void setRegistrationAuthorityAllowList(Set<String> registrationAuthorityAllowList) {
        this.updated = !Objects.equals(this.registrationAuthorityAllowList, registrationAuthorityAllowList);
        this.registrationAuthorityAllowList = registrationAuthorityAllowList;
    }

    public Map<String, List<String>> getCategoryDenyList() {
        return categoryDenyList;
    }

    public void setCategoryDenyList(Map<String, List<String>> categoryDenyList) {
        this.updated = !Objects.equals(this.categoryDenyList, categoryDenyList);
        this.categoryDenyList = categoryDenyList;
    }

    public Map<String, List<String>> getCategoryAllowList() {
        return categoryAllowList;
    }

    public void setCategoryAllowList(Map<String, List<String>> categoryAllowList) {
        this.updated = !Objects.equals(this.categoryAllowList, categoryAllowList);
        this.categoryAllowList = categoryAllowList;
    }

    public List<MapFederationMapperEntity> getFederationMappers() {
        return federationMappers;
    }

    public void setFederationMappers(List<MapFederationMapperEntity> federationMappers) {
        this.updated = !Objects.equals(this.federationMappers, federationMappers);
        this.federationMappers = federationMappers;
    }

    public void addFederationMapper(MapFederationMapperEntity federationMapper) {
        this.updated = true;
        this.federationMappers.add(federationMapper);
    }

    public void removeFederationMapper(String id) {
        this.updated = true;
        MapFederationMapperEntity federationMapper = new MapFederationMapperEntity(id);
        this.federationMappers.remove(federationMapper);
    }
    
    public List<String> getIdps() {
        return idps;
    }

    public void setIdps(List<String> idps) {
        this.updated = !Objects.equals(this.idps, idps);
        this.idps = idps;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public int hashCode() {
        return getInternalId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapFederationEntity)) return false;
        final MapFederationEntity other = (MapFederationEntity) obj;
        return Objects.equals(other.getInternalId(), getInternalId());
    }
}
