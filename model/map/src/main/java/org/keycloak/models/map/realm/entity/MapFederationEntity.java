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
import org.keycloak.models.IdentityProviderModel;
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
    private Set<String> entityIdBlackList= new HashSet<>();
    private Set<String> entityIdWhiteList= new HashSet<>();
    private Set<String> registrationAuthorityBlackList= new HashSet<>();
    private Set<String> registrationAuthorityWhiteList= new HashSet<>();
    private Map<String, List<String>> categoryBlackList= new HashMap<>();
    private Map<String, List<String>> categoryWhiteList= new HashMap<>();
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
        entity.setEntityIdBlackList(model.getEntityIdBlackList());
        entity.setEntityIdWhiteList(model.getEntityIdWhiteList());
        entity.setCategoryBlackList(model.getCategoryBlackList());
        entity.setCategoryWhiteList(model.getCategoryWhiteList());
        entity.setRegistrationAuthorityBlackList(model.getRegistrationAuthorityBlackList());
        entity.setRegistrationAuthorityWhiteList(model.getRegistrationAuthorityWhiteList());
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
        model.setEntityIdBlackList(entity.getEntityIdBlackList());
        model.setEntityIdWhiteList(entity.getEntityIdWhiteList());
        model.setCategoryBlackList(entity.getCategoryBlackList());
        model.setCategoryWhiteList(entity.getCategoryWhiteList());
        model.setRegistrationAuthorityBlackList(entity.getRegistrationAuthorityBlackList());
        model.setRegistrationAuthorityWhiteList(entity.getRegistrationAuthorityWhiteList());
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

    public Set<String> getEntityIdBlackList() {
        return entityIdBlackList;
    }

    public void setEntityIdBlackList(Set<String> entityIdBlackList) {
        this.updated = !Objects.equals(this.entityIdBlackList, entityIdBlackList);
        this.entityIdBlackList = entityIdBlackList;
    }

    public Set<String> getEntityIdWhiteList() {
        return entityIdWhiteList;
    }

    public void setEntityIdWhiteList(Set<String> entityIdWhiteList) {
        this.updated = !Objects.equals(this.entityIdWhiteList, entityIdWhiteList);
        this.entityIdWhiteList = entityIdWhiteList;
    }

    public Set<String> getRegistrationAuthorityBlackList() {
        return registrationAuthorityBlackList;
    }

    public void setRegistrationAuthorityBlackList(Set<String> registrationAuthorityBlackList) {
        this.updated = !Objects.equals(this.registrationAuthorityBlackList, registrationAuthorityBlackList);
        this.registrationAuthorityBlackList = registrationAuthorityBlackList;
    }

    public Set<String> getRegistrationAuthorityWhiteList() {
        return registrationAuthorityWhiteList;
    }

    public void setRegistrationAuthorityWhiteList(Set<String> registrationAuthorityWhiteList) {
        this.updated = !Objects.equals(this.registrationAuthorityWhiteList, registrationAuthorityWhiteList);
        this.registrationAuthorityWhiteList = registrationAuthorityWhiteList;
    }

    public Map<String, List<String>> getCategoryBlackList() {
        return categoryBlackList;
    }

    public void setCategoryBlackList(Map<String, List<String>> categoryBlackList) {
        this.updated = !Objects.equals(this.categoryBlackList, categoryBlackList);
        this.categoryBlackList = categoryBlackList;
    }

    public Map<String, List<String>> getCategoryWhiteList() {
        return categoryWhiteList;
    }

    public void setCategoryWhiteList(Map<String, List<String>> categoryWhiteList) {
        this.updated = !Objects.equals(this.categoryWhiteList, categoryWhiteList);
        this.categoryWhiteList = categoryWhiteList;
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
