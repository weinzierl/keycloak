package org.keycloak.services.resources.admin;

import java.util.stream.Stream;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.AdminRoles;
import org.keycloak.models.GroupModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserGroupMembershipRequestModel;
import org.keycloak.models.UserModel;
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

    @POST
    @Path("{id}/action")
    public void changeStatus(@PathParam("id") String id, @QueryParam("status") String status) {
        auth.adminAuth().hasRealmRole(AdminRoles.MANAGE_USERS);
        UserGroupMembershipRequestModel request = realm.changeStatusUserGroupMembershipRequest(id,auth.adminAuth().getUser().getId(),status.toUpperCase());
        if ( request == null)
            throw new NotFoundException("UserGroupMembershipRequestEntity not found");
        adminEvent.operation(OperationType.ACTION).representation(ModelToRepresentation.toRepresentation(request, session, realm)).resourcePath(session.getContext().getUri()).success();
        if ( "approved".equals(status)) {
            UserModel user = session.users().getUserById(realm, request.getUserId());
            if (user == null) {
                throw new NotFoundException("User not found");
            }
            auth.users().requireManageGroupMembership(user);

            GroupModel group = session.groups().getGroupById(realm, request.getGroupId());
            if (group == null) {
                throw new NotFoundException("Group not found");
            }
            auth.groups().requireManageMembership(group);

            if (!user.isMemberOf(group)){
                user.joinGroup(group);
                adminEvent.operation(OperationType.CREATE).resource(ResourceType.GROUP_MEMBERSHIP).representation(ModelToRepresentation.toRepresentation(group, true)).resourcePath(session.getContext().getUri()).success();
            }
        }
    }

}
