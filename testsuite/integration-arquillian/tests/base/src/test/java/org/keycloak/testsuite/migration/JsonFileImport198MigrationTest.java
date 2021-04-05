/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.testsuite.migration;

import org.junit.Test;
import org.keycloak.exportimport.util.ImportUtils;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.keycloak.testsuite.arquillian.annotation.AuthServerContainerExclude.AuthServer;

/**
 * Tests that we can import json file from previous version.  MigrationTest only tests DB.
 *
 * @author <a href="mailto:bill@burkecentral.com">Bill Burke</a>
 * @version $Revision: 1 $
 */
@AuthServerContainerExclude(value = {AuthServer.REMOTE, AuthServer.QUARKUS}, details = "It works locally for Quarkus, but failing on CI for unknown reason")
public class JsonFileImport198MigrationTest extends AbstractJsonFileImportMigrationTest {

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {
        Map<String, RealmRepresentation> reps = null;
        try {
            reps = ImportUtils.getRealmsFromStream(JsonSerialization.mapper, IOUtil.class.getResourceAsStream("/migration-test/migration-realm-1.9.8.Final.json"));
            masterRep = reps.remove("master");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        for (RealmRepresentation rep : reps.values()) {
            testRealms.add(rep);
        }


    }

    @Test
    public void migration1_9_8Test() throws Exception {
        checkRealmsImported();
        testMigratedMigrationData(false);
        testMigrationTo2_0_0();
        testMigrationTo2_1_0();
        testMigrationTo2_2_0();
        testMigrationTo2_3_0();
        testMigrationTo2_5_0();
        testMigrationTo3_x();
        testMigrationTo4_x(false, false);
        testMigrationTo5_x();
        testMigrationTo6_x();
        testMigrationTo7_x(false);
        testMigrationTo8_x();
        testMigrationTo9_x();
        testMigrationTo12_x(false);
        testMigrationTo16_x();
    }

    @Override
    protected void testMigrationTo2_3_0() {
        testUpdateProtocolMappers(migrationRealm);
        testExtractRealmKeysMigrationRealm(migrationRealm);
    }

}
