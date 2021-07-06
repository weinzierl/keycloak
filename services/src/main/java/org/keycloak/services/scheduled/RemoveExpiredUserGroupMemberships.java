package org.keycloak.services.scheduled;

import org.keycloak.models.KeycloakSession;
import org.keycloak.models.UserModel;
import org.keycloak.timer.ScheduledTask;

public class RemoveExpiredUserGroupMemberships implements ScheduledTask {

    @Override
    public void run(KeycloakSession session) {
        session.realms().getRealmsStream().forEach(realm -> session.users().getUsersStream(realm).forEach(UserModel::removeExpiredGroups));
    }

}