package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.AccountRoles;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.representations.idm.RealmRepresentation;

public class MigrateTo16_0_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("16.0.0");

    @Override
    public void migrate(KeycloakSession session) {

        session.realms().getRealmsStream().forEach(this::changeManageAccount);
    }

    @Override
    public void migrateImport(KeycloakSession session, RealmModel realm, RealmRepresentation rep, boolean skipUserDependent) {
        changeManageAccount(realm);
    }

    private void changeManageAccount(RealmModel realm) {
        ClientModel accountClient = realm.getClientByClientId(Constants.ACCOUNT_MANAGEMENT_CLIENT_ID);
        if (accountClient != null){
            if (accountClient.getRole(AccountRoles.MANAGE_ACCOUNT_EMAIL) == null) {
                RoleModel manageEmail = accountClient.addRole(AccountRoles.MANAGE_ACCOUNT_EMAIL);
                manageEmail.setDescription("${role_" + AccountRoles.MANAGE_ACCOUNT_EMAIL + "}");
            }
            if (accountClient.getRole(AccountRoles.MANAGE_ACCOUNT_NAME) == null) {
                RoleModel manageName = accountClient.addRole(AccountRoles.MANAGE_ACCOUNT_NAME);
                manageName.setDescription("${role_" + AccountRoles.MANAGE_ACCOUNT_NAME + "}");
            }
        }
    }

    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }

}