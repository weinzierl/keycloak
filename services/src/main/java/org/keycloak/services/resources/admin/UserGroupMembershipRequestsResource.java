package org.keycloak.services.resources.admin;

import java.util.stream.Stream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserGroupMembershipRequestModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.UserGroupMembershipRequestRepresentation;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;

public class UserGroupMembershipRequestsResource {

    private static final Logger logger = Logger.getLogger(UsersResource.class);

    protected RealmModel realm;

    private AdminPermissionEvaluator auth;

    private AdminEventBuilder adminEvent;

    @Context
    protected KeycloakSession session;

    public UserGroupMembershipRequestsResource(RealmModel realm, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.auth = auth;
        this.realm = realm;
        this.adminEvent = adminEvent.resource(ResourceType.USER_GROUP_REQUESTS);
    }

    @GET
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<UserGroupMembershipRequestRepresentation> getRequests(@QueryParam("pending") @DefaultValue("true") Boolean pending,
                                                                        @QueryParam("first") Integer firstResult,
                                                                        @QueryParam("max") Integer maxResults) {
        auth.adminAuth().hasRealmRole(AdminRoles.MANAGE_USERS);
        Stream<UserGroupMembershipRequestModel> requests = pending ? realm.getUserGroupMembershipRequestsByStatus("PENDING", firstResult, maxResults) : realm.getUserGroupMembershipRequests(firstResult, maxResults);
        return requests.map(model -> ModelToRepresentation.toRepresentation(model, session, realm));
    }

}
