package org.keycloak.migration.migrators;

import org.keycloak.migration.ModelVersion;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.RealmRepresentation;

import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_2FA;
import static org.keycloak.models.AccountRoles.MANAGE_ACCOUNT_BASIC_AUTH;
import static org.keycloak.models.AccountRoles.VIEW_GROUPS;

public class MigrateTo16_1_0_2_0 implements Migration {

    public static final ModelVersion VERSION = new ModelVersion("16.1.0-2.0");

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

    }


    @Override
    public ModelVersion getVersion() {
        return VERSION;
    }
}
