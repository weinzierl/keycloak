package org.keycloak.representations.idm;

import java.util.List;

public class JoinGroupRequestRepresentation {

    private String reason;
    private List<String> joinGroups;

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<String> getJoinGroups() {
        return joinGroups;
    }

    public void setJoinGroups(List<String> joinGroups) {
        this.joinGroups = joinGroups;
    }
}
