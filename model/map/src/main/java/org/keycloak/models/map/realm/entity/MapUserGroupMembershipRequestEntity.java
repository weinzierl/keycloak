package org.keycloak.models.map.realm.entity;

import java.util.HashMap;
import java.util.Objects;

import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.UserGroupMembershipRequestModel;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.utils.KeycloakModelUtils;

public class MapUserGroupMembershipRequestEntity implements UpdatableEntity {

    protected String id;
    protected String userId;
    protected String groupId;
    protected String status;
    protected String reason;
    protected String viewerId;
    private boolean updated;

    private MapUserGroupMembershipRequestEntity() {}

    public static MapUserGroupMembershipRequestEntity fromModel(UserGroupMembershipRequestModel model) {
        if (model == null) return null;
        MapUserGroupMembershipRequestEntity entity = new MapUserGroupMembershipRequestEntity();

        entity.setId(KeycloakModelUtils.generateId());
        entity.setUserId(model.getUserId());
        entity.setGroupId(model.getGroupId());
        entity.setReason(model.getReason());
        entity.setStatus(model.getStatus());
        entity.setViewerId(model.getViewerId());

        return entity;
    }

    public static UserGroupMembershipRequestModel toModel(MapUserGroupMembershipRequestEntity entity) {
        if (entity == null) return null;
        UserGroupMembershipRequestModel model = new UserGroupMembershipRequestModel();

        model.setId(entity.getId());
        model.setUserId(entity.getUserId());
        model.setGroupId(entity.getGroupId());
        model.setReason(entity.getReason());
        model.setStatus(entity.getStatus());
        model.setViewerId(entity.getViewerId());

        return model;
    }

    @Override
    public boolean isUpdated() {
        return updated;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.updated = !Objects.equals(this.id, id);
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.updated = !Objects.equals(this.userId, userId);
        this.userId = userId;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.updated = !Objects.equals(this.groupId, groupId);
        this.groupId = groupId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.updated = !Objects.equals(this.status, status);
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.updated = !Objects.equals(this.reason, reason);
        this.reason = reason;
    }

    public String getViewerId() {
        return viewerId;
    }

    public void setViewerId(String viewerId) {
        this.updated = !Objects.equals(this.viewerId, viewerId);
        this.viewerId = viewerId;
    }

    @Override
    public int hashCode() {
        return getId().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof MapUserGroupMembershipRequestEntity)) return false;
        final MapUserGroupMembershipRequestEntity other = (MapUserGroupMembershipRequestEntity) obj;
        return Objects.equals(other.getId(), getId());
    }

}
