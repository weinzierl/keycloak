package org.keycloak.models;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;

public class UserGroupMembershipModel implements Serializable, Comparable<UserGroupMembershipModel> {

    protected GroupModel group;
    protected Long validThrough;

    public UserGroupMembershipModel (GroupModel group ) {
        this.group = group;
    }

    public UserGroupMembershipModel (GroupModel group, Long validThrough ) {
        this.group = group;
        this.validThrough = validThrough;
    }

    public GroupModel getGroup() {
        return group;
    }

    public void setGroup(GroupModel group) {
        this.group = group;
    }

    public Long getValidThrough() {
        return validThrough;
    }

    public void setValidThrough(Long validThrough) {
        this.validThrough = validThrough;
    }

    @Override
    public int compareTo(UserGroupMembershipModel o) {
        if(o == null)
            return -1;

        return this.group.getId().compareTo(o.getGroup().getId());
    }
}
