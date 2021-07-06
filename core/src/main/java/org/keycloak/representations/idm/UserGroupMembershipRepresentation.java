package org.keycloak.representations.idm;

import java.util.SortedMap;

public class UserGroupMembershipRepresentation {

    protected GroupRepresentation group;
    protected Long validThrough;

    public GroupRepresentation getGroup() {
        return group;
    }

    public void setGroup(GroupRepresentation group) {
        this.group = group;
    }

    public Long getValidThrough() {
        return validThrough;
    }

    public void setValidThrough(Long validThrough) {
        this.validThrough = validThrough;
    }

}
