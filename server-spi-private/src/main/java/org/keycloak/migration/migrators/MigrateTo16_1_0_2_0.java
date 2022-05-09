package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_2FA;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH;

public class MigrateTo16_1_0_2_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("16.1.0-2.0");

    @Override
    public void migrate(KeycloakSession session) {

        session.realms().getRealmsStream().forEach(this::migrateRealm);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        migrateRealm(realm);
    }

    private void migrateRealm(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null && accountClient.getRole(AccountRoles.VIEW_GROUPS) == null) {
            RoleModel viewAppRole = accountClient.addRole(AccountRoles.VIEW_GROUPS);
            viewAppRole.setDescription("${role_" + AccountRoles.VIEW_GROUPS + "}");
        }
        accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null && accountClient.getRole(MANAGE_ACCOUNT_BASIC_AUTH) == null) {
            RoleModel viewAppRole = accountClient.addRole(MANAGE_ACCOUNT_BASIC_AUTH);
            viewAppRole.setDescription("${role_" + MANAGE_ACCOUNT_BASIC_AUTH + "}");
        }
        if (accountClient != null && accountClient.getRole(MANAGE_ACCOUNT_2FA) == null) {
            RoleModel manageAccount2fa = accountClient.addRole(MANAGE_ACCOUNT_2FA);
            manageAccount2fa.setDescription("${role_" + MANAGE_ACCOUNT_2FA + "}");
        }
        realm.setClaimsSupported(RepresentationToModel.DEFAULT_CLAIMS_SUPPORTED);
    }


    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
