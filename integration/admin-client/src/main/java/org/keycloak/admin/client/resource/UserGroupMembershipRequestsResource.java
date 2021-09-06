package org.keycloak.admin.client.resource;

import java.util.List;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

import org.keycloak.representations.idm.UserGroupMembershipRequestRepresentation;

public interface UserGroupMembershipRequestsResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<UserGroupMembershipRequestRepresentation> getRequests(@QueryParam("pending") Boolean pending,
                                                                      @QueryParam("first") Integer firstResult,
                                                                      @QueryParam("max") Integer maxResults) ;

    @POST
    @Path("{id}/action")
    public void changeStatus(@PathParam("id") String id, @QueryParam("status") String status);
}
