package org.keycloak.migration.migrators;

import java.util.Objects;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_2FA;
import static org.keycloak.models.AccountRoles.VIEW_GROUPS;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH;
import static org.keycloak.models.Constants.ACCOUNT_MANAGEMENT_CLIENT_ID;

public class MigrateTo16_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("16.0.0");

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        addGroupsRole(realm);
    }

    @Override
    public void migrate(KeycloakSession session) {
        session.realms()
                .getRealmsStream()
                .forEach(this::addGroupsRole);
    }

    private void addGroupsRole(RealmModel realm) {
        realm.setClaimsSupported(RepresentationToModel.DEFAULT_CLAIMS_SUPPORTED);
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null && accountClient.getRole(MANAGE_ACCOUNT_BASIC_AUTH) == null) {
            RoleModel viewAppRole = accountClient.addRole(MANAGE_ACCOUNT_BASIC_AUTH);
            viewAppRole.setDescription("${role_" + MANAGE_ACCOUNT_BASIC_AUTH + "}");
        }
        if (accountClient != null && accountClient.getRole(MANAGE_ACCOUNT_2FA) == null) {
            RoleModel manageAccount2fa = accountClient.addRole(MANAGE_ACCOUNT_2FA);
            manageAccount2fa.setDescription("${role_" + MANAGE_ACCOUNT_2FA + "}");
        }
        if (accountClient != null && accountClient.getRole(VIEW_GROUPS) == null) {
            RoleModel manageAccountBasicAuth = accountClient.addRole(VIEW_GROUPS);
            manageAccountBasicAuth.setDescription("${role_" + VIEW_GROUPS + "}");
        }
    }


    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
