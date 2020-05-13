/*
 * Copyright 2021 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.realm.entity;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class MapIdpFederationEntity implements UpdatableEntity {

    protected String internalId;
    private String url;
    private String alias;
    private String displayName;
    private String providerId;
    private Integer updateFrequencyInMins;
    private Long validUntilTimestamp;
    private Long lastMetadataRefreshTimestamp;
    private Map<String, String> config;
    private Set<String> skipEntities = new HashSet<String>();

    private boolean updated;

    private MapIdpFederationEntity() {}



    public static MapIdpFederationEntity fromModel(IdentityProvidersFederationModel model) {
        if (model == null) return null;
        MapIdpFederationEntity entity = new MapIdpFederationEntity();
        String id = model.getInternalId() == null ? KeycloakModelUtils.generateId() : model.getInternalId();
        entity.setInternalId(id);
        entity.setAlias(model.getAlias());
        entity.setDisplayName(model.getDisplayName());
        entity.setProviderId(model.getProviderId());
        entity.setUrl(model.getUrl());
        entity.setUpdateFrequencyInMins(model.getUpdateFrequencyInMins());
        entity.setValidUntilTimestamp(model.getValidUntilTimestamp());
        entity.setLastMetadataRefreshTimestamp(model.getLastMetadataRefreshTimestamp());
        entity.setSkipEntities(model.getSkipIdps());
        entity.setConfig(model.getConfig() == null ? null : new HashMap<>(model.getConfig()));
        return entity;
    }

    public static IdentityProvidersFederationModel toModel(MapIdpFederationEntity entity) {
        if (entity == null) return null;
        IdentityProvidersFederationModel model = new IdentityProvidersFederationModel();
        model.setInternalId(entity.getInternalId());
        model.setAlias(entity.getAlias());
        model.setDisplayName(entity.getDisplayName());
        model.setProviderId(entity.getProviderId());
        model.setUrl(entity.getUrl());
        model.setUpdateFrequencyInMins(entity.getUpdateFrequencyInMins());
        model.setValidUntilTimestamp(entity.getValidUntilTimestamp());
        model.setLastMetadataRefreshTimestamp(entity.getLastMetadataRefreshTimestamp());
        model.setSkipIdps(entity.getSkipEntities());
        model.setConfig(entity.getConfig() == null ? null : new HashMap<>(entity.getConfig()));
        return model;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    public String getInternalId() {
        return internalId;
    }

    public void setInternalId(String internalId) {
        this.updated = !Objects.equals(this.internalId, internalId);
        this.internalId = internalId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.updated = !Objects.equals(this.url, url);
        this.url = url;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.updated = !Objects.equals(this.alias, alias);
        this.alias = alias;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.updated = !Objects.equals(this.displayName, displayName);
        this.displayName = displayName;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.updated = !Objects.equals(this.providerId, providerId);
        this.providerId = providerId;
    }

    public Integer getUpdateFrequencyInMins() {
        return updateFrequencyInMins;
    }

    public void setUpdateFrequencyInMins(Integer updateFrequencyInMins) {
        this.updated = !Objects.equals(this.updateFrequencyInMins, updateFrequencyInMins);
        this.updateFrequencyInMins = updateFrequencyInMins;
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

    public Set<String> getSkipEntities() {
        return skipEntities;
    }

    public void setSkipEntities(Set<String> skipEntities) {
        this.updated = !Objects.equals(this.skipEntities, skipEntities);
        this.skipEntities = skipEntities;
    }


    public void setUpdated(boolean updated) {
        this.updated = updated;
    }


//
//    @Override
//    public int hashCode() {
//        return getId().hashCode();
//    }
//
//    @Override
//    public boolean equals(Object obj) {
//        if (this == obj) return true;
//        if (!(obj instanceof MapIdpFederationEntity)) return false;
//        final MapIdpFederationEntity other = (MapIdpFederationEntity) obj;
//        return Objects.equals(other.getId(), getId());
//    }
}
