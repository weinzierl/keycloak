package org.keycloak.representations.idm;

public class UserMembershipRepresentation {

    protected UserRepresentation user;
    protected Long validFrom;
    protected Long validThrough;

    public UserMembershipRepresentation() {
    }

    public UserMembershipRepresentation(UserRepresentation user) {
        this.user = user;
    }

    public UserRepresentation getUser() {
        return user;
    }

    public void setUser(UserRepresentation user) {
        this.user = user;
    }

    public Long getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Long validFrom) {
        this.validFrom = validFrom;
    }

    public Long getValidThrough() {
        return validThrough;
    }

    public void setValidThrough(Long validThrough) {
        this.validThrough = validThrough;
    }
}
