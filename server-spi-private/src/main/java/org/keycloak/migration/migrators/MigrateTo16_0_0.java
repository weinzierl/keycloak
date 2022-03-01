package org.keycloak.migration.migrators;

import java.util.Objects;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_2FA;
import static org.keycloak.models.AccountRoles.VIEW_GROUPS;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;

public class MigrateTo16_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("16.0.0");

    @Override
    public void migrate(KeycloakSession session) {
        session.realms()
                .getRealmsStream()
                .map(realm -> realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID))
                .filter(Objects::nonNull)
                .filter(client -> Objects.isNull(client.getRole(MANAGE_ACCOUNT_BASIC_AUTH)))
                .forEach(client -> client.addRole(MANAGE_ACCOUNT_BASIC_AUTH)
                        .setDescription("${role_" + MANAGE_ACCOUNT_BASIC_AUTH + "}"));

        session.realms()
                .getRealmsStream()
                .map(realm -> realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID))
                .filter(Objects::nonNull)
                .filter(client -> Objects.isNull(client.getRole(MANAGE_ACCOUNT_2FA)))
                .forEach(client -> client.addRole(MANAGE_ACCOUNT_2FA)
                        .setDescription("${role_" + MANAGE_ACCOUNT_2FA + "}"));

        session.realms()
                .getRealmsStream()
                .map(realm -> realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID))
                .filter(Objects::nonNull)
                .filter(client -> Objects.isNull(client.getRole(VIEW_GROUPS)))
                .forEach(client -> client.addRole(VIEW_GROUPS)
                        .setDescription("${role_" + VIEW_GROUPS + "}"));
    }


    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
