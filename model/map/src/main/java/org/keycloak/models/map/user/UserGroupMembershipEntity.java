package org.keycloak.models.map.user;

import java.util.Objects;

import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserGroupMembershipModel;
import org.keycloak.models.map.common.UpdatableEntity;

public class UserGroupMembershipEntity implements UpdatableEntity {

    private String groupId;
    private Long validThrough;
    private boolean updated;

    protected UserGroupMembershipEntity() {}

    public static UserGroupMembershipEntity fromModel(UserGroupMembershipModel model) {

        UserGroupMembershipEntity entity = new UserGroupMembershipEntity();
        entity.setGroupId(model.getGroup().getId());
        entity.setValidThrough(model.getValidThrough());

        return entity;
    }

    public static UserGroupMembershipModel toModel(KeycloakSession session, RealmModel realm, UserGroupMembershipEntity entity) {
        if (entity == null) {
            return null;
        }

        GroupModel group = session.groups().getGroupById(realm, entity.getGroupId());
        if (group == null) {
            throw new ModelException("Group with id " + entity.getGroupId() + " is not available");
        }
        UserGroupMembershipModel model = new UserGroupMembershipModel(group, entity.getValidThrough());

        return model;
    }


    @Override
    public boolean isUpdated() {
        return updated;
    }

    public String getGroupId() {
        return groupId;
    }

    public void setGroupId(String groupId) {
        this.updated = !Objects.equals(this.groupId, groupId);
        this.groupId = groupId;
    }

    public Long getValidThrough() {
        return validThrough;
    }

    public void setValidThrough(Long validThrough) {
        this.updated = !Objects.equals(this.validThrough, validThrough);
        this.validThrough = validThrough;
    }
}
