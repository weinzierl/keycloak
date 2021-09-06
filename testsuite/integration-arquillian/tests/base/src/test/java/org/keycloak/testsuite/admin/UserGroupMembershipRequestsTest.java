package org.keycloak.testsuite.admin;

import java.io.IOException;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.type.TypeReference;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.broker.provider.util.SimpleHttp;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.JoinGroupRequestRepresentation;
import org.keycloak.representations.idm.RealmRepresentation;
import org.keycloak.representations.idm.UserGroupMembershipRequestRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.keycloak.testsuite.AbstractKeycloakTest;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.testsuite.util.AssertAdminEvents;
import org.keycloak.testsuite.util.TokenUtil;
import org.keycloak.testsuite.util.UserBuilder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.keycloak.testsuite.utils.io.IOUtil.loadRealm;

public class UserGroupMembershipRequestsTest extends AbstractKeycloakTest {

    @Rule
    public TokenUtil tokenUtil = new TokenUtil("direct-login", "password");

    @Rule
    public AssertAdminEvents assertAdminEvents = new AssertAdminEvents(this);

    private CloseableHttpClient client;

    @Before
    public void before() {
        client = HttpClientBuilder.create().build();
    }

    @After
    public void after() {
        try {
            client.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void addTestRealms(List<RealmRepresentation> testRealms) {

        RealmRepresentation testRealmRep = loadRealm("/testrealm.json");
        testRealms.add(testRealmRep);

        testRealmRep.setEventsEnabled(true);

        List<UserRepresentation> users = testRealmRep.getUsers();

        UserRepresentation user = UserBuilder.create().username("direct-login").email("direct-login@localhost").role("account", "manage-account").password("password").build();
        users.add(user);

        GroupRepresentation group = new GroupRepresentation();
        group.setName("Group A");
        testRealmRep.getGroups().add(group);

        GroupRepresentation groupb = new GroupRepresentation();
        groupb.setName("Group B");
        testRealmRep.getGroups().add(groupb);
    }

    @Test
    public void testRequests() throws IOException {
        RealmResource realm = adminClient.realms().realm("test");
        GroupRepresentation group = realm.getGroupByPath("Group A");
        getCleanup().addGroupId(group.getId());
        GroupRepresentation groupb = realm.getGroupByPath("Group B");
        getCleanup().addGroupId(groupb.getId());
        UserRepresentation user = realm.users().search("direct-login").get(0);
        getCleanup().addUserId(user.getId());

        //approve first request
        JoinGroupRequestRepresentation request = new JoinGroupRequestRepresentation();
        request.setJoinGroups(Stream.of(group.getId()).collect(Collectors.toList()));
        SimpleHttp.doPost(getAccountUrl("groups/join"), client).json(request).auth(tokenUtil.getToken()).acceptJson().asResponse();
        List<UserGroupMembershipRequestRepresentation> requests = realm.requests().getRequests(true, 0, 20);
        assertEquals(1, requests.size());
        realm.requests().changeStatus(requests.get(0).getId(), "approved");
        assertAdminEvents.assertEvent("test", OperationType.CREATE, AdminEventPaths.changeRequestStatus(requests.get(0).getId()), ResourceType.GROUP_MEMBERSHIP);
        assertAdminEvents.assertEvent("test", OperationType.ACTION, AdminEventPaths.changeRequestStatus(requests.get(0).getId()), ResourceType.USER_GROUP_REQUESTS);
        List<GroupRepresentation> memberships = realm.users().get(user.getId()).groups();
        assertEquals(1, memberships.size());
        assertEquals("Group A", memberships.get(0).getName());

        //reject second request
        JoinGroupRequestRepresentation requestb = new JoinGroupRequestRepresentation();
        requestb.setJoinGroups(Stream.of(groupb.getId()).collect(Collectors.toList()));
        SimpleHttp.doPost(getAccountUrl("groups/join"), client).json(requestb).auth(tokenUtil.getToken()).acceptJson().asResponse();
        requests = realm.requests().getRequests(true, 0, 20);
        assertEquals(1, requests.size());
        realm.requests().changeStatus(requests.get(0).getId(), "rejected");
        assertAdminEvents.assertEvent("test", OperationType.ACTION, AdminEventPaths.changeRequestStatus(requests.get(0).getId()), ResourceType.USER_GROUP_REQUESTS);
        memberships = realm.users().get(user.getId()).groups();
        assertEquals(1, memberships.size());
        assertEquals("Group A", memberships.get(0).getName());

        //chech that exists two requests - noone pending
        requests = realm.requests().getRequests(true, 0, 20);
        assertTrue( requests.isEmpty());
        requests = realm.requests().getRequests(false, 0, 20);
        assertEquals(2, requests.size());
    }

    private String getAccountUrl(String resource) {
        return suiteContext.getAuthServerInfo().getContextRoot().toString() + "/auth/realms/test/account" + (resource != null ? "/" + resource : "");
    }
}
