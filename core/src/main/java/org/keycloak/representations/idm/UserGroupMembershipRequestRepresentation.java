package org.keycloak.representations.idm;

public class UserGroupMembershipRequestRepresentation {

    protected String id;
    protected UserRepresentation user;
    protected GroupRepresentation group;
    protected String status;
    protected String reason;
    protected UserRepresentation viewer;


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public GroupRepresentation getGroup() {
        return group;
    }

    public void setGroup(GroupRepresentation group) {
        this.group = group;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public UserRepresentation getViewer() {
        return viewer;
    }

    public void setViewer(UserRepresentation viewer) {
        this.viewer = viewer;
    }
}
