package org.keycloak.protocol.oidc.federation.rest.op;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.keycloak.models.KeycloakSession;

public class FederationOPService {

	private KeycloakSession session;

    public FederationOPService(KeycloakSession session) {
        this.session = session;
    }
    
    
    @GET
    @Path("fedreg")
    @Produces("text/plain; charset=utf-8")
    public String getFederationRegistration() {
        String name = session.getContext().getRealm().getDisplayName();
        if (name == null) {
            name = session.getContext().getRealm().getName();
        }
        return "Hello " + name;
    }
	
    @POST
    @Path("par")
    @Produces("text/plain; charset=utf-8")
    public String postPushedAuthorization() {
        String name = session.getContext().getRealm().getDisplayName();
        if (name == null) {
            name = session.getContext().getRealm().getName();
        }
        return "Hello " + name;
    }
    
    
    
}
