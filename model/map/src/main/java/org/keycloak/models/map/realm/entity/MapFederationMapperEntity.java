package org.keycloak.models.map.realm.entity;

import java.util.Map;
import java.util.Objects;

import org.keycloak.models.FederationMapperModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapFederationMapperEntity implements UpdatableEntity {

    private String id;
    private String name;
    private String identityProviderMapper;
    private Map<String, String> config;

    private boolean updated;

    private MapFederationMapperEntity() {
    }

    public MapFederationMapperEntity(String id) {
        this.id = id;
    }

    public static MapFederationMapperEntity fromModel(FederationMapperModel model) {
        if (model == null) return null;
        MapFederationMapperEntity entity = new MapFederationMapperEntity();
        if ( model.getId() == null)
            model.setId( KeycloakModelUtils.generateId());
        entity.setId(model.getId());
        entity.setName(model.getName());
        entity.setIdentityProviderMapper(model.getIdentityProviderMapper());
        entity.setConfig(model.getConfig());

        return entity;
    }

    public static FederationMapperModel toModel(MapFederationMapperEntity entity) {
        if (entity == null) return null;
        FederationMapperModel model = new FederationMapperModel();

        model.setId(entity.getId());
        model.setName(entity.getName());
        model.setIdentityProviderMapper(entity.getIdentityProviderMapper());
        model.setConfig(entity.getConfig());

        return model;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.updated = !Objects.equals(this.id, id);
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.updated = !Objects.equals(this.name, name);
        this.name = name;
    }

    public String getIdentityProviderMapper() {
        return identityProviderMapper;
    }

    public void setIdentityProviderMapper(String identityProviderMapper) {
        this.updated = !Objects.equals(this.identityProviderMapper, identityProviderMapper);
        this.identityProviderMapper = identityProviderMapper;
    }

    public Map<String, String> getConfig() {
        return config;
    }

    public void setConfig(Map<String, String> config) {
        this.updated = !Objects.equals(this.config, config);
        this.config = config;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapFederationMapperEntity)) return false;
        final MapFederationMapperEntity other = (MapFederationMapperEntity) obj;
        return Objects.equals(other.getId(), getId());
    }
}
