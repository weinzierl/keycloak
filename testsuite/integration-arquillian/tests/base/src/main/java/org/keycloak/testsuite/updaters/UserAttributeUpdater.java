package org.keycloak.testsuite.updaters;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.models.UserModel;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.UserGroupMembershipRepresentation;
import org.keycloak.representations.idm.UserRepresentation;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

/**
 * Updater for user attributes. See {@link ServerResourceUpdater} for further details.
 * @author hmlnarik
 */
public class UserAttributeUpdater extends ServerResourceUpdater<UserAttributeUpdater, UserResource, UserRepresentation> {

    private final RealmResource realmResource;

    /**
     * Creates a {@UserAttributeUpdater} for the given user. The user must exist.
     * @param adminClient
     * @param realm
     * @param clientId
     * @return
     */
    public static UserAttributeUpdater forUserByUsername(Keycloak adminClient, String realm, String userName) {
        return forUserByUsername(adminClient.realm(realm), userName);
    }

    public static UserAttributeUpdater forUserByUsername(RealmResource realm, String userName) {
        UsersResource users = realm.users();
        List<UserRepresentation> foundUsers = users.search(userName).stream()
          .filter(ur -> userName.equalsIgnoreCase(ur.getUsername()))
          .collect(Collectors.toList());
        assertThat(foundUsers, hasSize(1));
        UserResource userRes = users.get(foundUsers.get(0).getId());

        return new UserAttributeUpdater(userRes, realm);
    }

    public UserAttributeUpdater(UserResource resource) {
        this(resource, null);
    }

    public UserAttributeUpdater(UserResource resource, RealmResource realmResource) {
        super(resource,
          () -> {
            UserRepresentation r = resource.toRepresentation();
            r.setGroups(resource.groups());
            return r;
          },
          resource::update
        );
        if (this.rep.getAttributes() == null) {
            this.rep.setAttributes(new HashMap<>());
        }
        this.realmResource = realmResource;
    }

    @Override
    protected void performUpdate(UserRepresentation from, UserRepresentation to) {
        super.performUpdate(from, to);
        updateViaAddRemove(from.getGroups().stream().map(member -> member.getGroup().getPath()).collect(Collectors.toList()), to.getGroups().stream().map(member -> member.getGroup().getPath()).collect(Collectors.toList()), this::getConversionForGroupPathToId, group -> resource.joinGroup(group), resource::leaveGroup);
    }

    private Function<String, String> getConversionForGroupPathToId() {
        if (realmResource == null) {
            return String::toString;
        }

        Map<String, String> humanIdToIdMap = realmResource.groups().groups().stream()
            .collect(Collectors.toMap(GroupRepresentation::getPath, GroupRepresentation::getId));

        return humanIdToIdMap::get;
    }

    public UserAttributeUpdater setAttribute(String name, List<String> value) {
        this.rep.getAttributes().put(name, value);
        return this;
    }

    public UserAttributeUpdater setAttribute(String name, String... values) {
        this.rep.getAttributes().put(name, Arrays.asList(values));
        return this;
    }

    public UserAttributeUpdater removeAttribute(String name) {
        this.rep.getAttributes().put(name, null);
        return this;
    }

    public UserAttributeUpdater setEmailVerified(Boolean emailVerified) {
        rep.setEmailVerified(emailVerified);
        return this;
    }

    public UserAttributeUpdater setRequiredActions(UserModel.RequiredAction... requiredAction) {
        rep.setRequiredActions(Arrays.stream(requiredAction)
                .map(action -> action.name())
                .collect(Collectors.toList())
        );
        return this;
    }

    public RoleScopeUpdater realmRoleScope() {
        return new RoleScopeUpdater(resource.roles().realmLevel());
    }

    public RoleScopeUpdater clientRoleScope(String clientUUID) {
        return new RoleScopeUpdater(resource.roles().clientLevel(clientUUID));
    }

    /**
     * @param groups List of expected group paths
     * @return
     */
    public UserAttributeUpdater setGroups(String... groups) {
        List<UserGroupMembershipRepresentation> members = new ArrayList<>();
        for (int i = 0; i < groups.length; i++) {
            UserGroupMembershipRepresentation member = new UserGroupMembershipRepresentation();
            GroupRepresentation groupRep = new GroupRepresentation();
            groupRep.setPath(groups[i]);
            member.setGroup(groupRep);
            members.add(member);
        }
        rep.setGroups(members);
        return this;
    }
}
