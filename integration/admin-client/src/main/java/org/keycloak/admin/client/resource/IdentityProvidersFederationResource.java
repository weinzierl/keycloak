package org.keycloak.admin.client.resource;

import java.util.Map;
import java.util.Set;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;

public interface IdentityProvidersFederationResource {

	@POST
	@Path("import-config")
	@Consumes(MediaType.APPLICATION_JSON)
	@Produces(MediaType.APPLICATION_JSON)
	Set<String> importFrom(Map<String, Object> data);

	@POST
	@Path("instances")
	@Consumes(MediaType.APPLICATION_JSON)
	Response create(IdentityProvidersFederationRepresentation identityProviderFederationRepresentation);

	@GET
	@Path("instances/{id}")
	@Produces(MediaType.APPLICATION_JSON)
	public IdentityProvidersFederationRepresentation getIdentityProviderFederation(@PathParam("id") String internalId);

	@DELETE
	@Path("delete/{id}")
	@Consumes(MediaType.APPLICATION_JSON)
	public Response delete(@PathParam("id") String internalId);
}
