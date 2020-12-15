package org.keycloak.protocol.oidc.federation.rp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.federation.rp.broker.OIDCFedIdentityProviderConfig;

public class OIDCFedIdPResource {

    private static final Logger logger = Logger.getLogger(OIDCFedIdPResource.class);

    private KeycloakSession session;
    private OIDCFedIdentityProviderConfig config;
    private  RealmModel realm;


    public OIDCFedIdPResource(KeycloakSession session, RealmModel realm, OIDCFedIdentityProviderConfig config) {
        this.session = session;
        this.config = config;
    }


    @GET
    @Path(".well-known/oidc-federation")
    @Produces(MediaType.APPLICATION_JSON)
    public String getConfiguration() {

        //form here the response of the well-known
        return String.format("{\"status\": \"Success\", \"realm\": \"%s\"}", realm.getName());
    }
    
    @GET
    @Path("explicit-registration")
    @Produces(MediaType.APPLICATION_JSON)
    public Response excplicitRegistration() {
        if (!"explicit".equals(config.getClientRegistrationTypes()))
            return Response.status(Response.Status.BAD_REQUEST).entity("This OIDC Federation RP does not support excplicit registration").build();

        //form here the response of the well-known
        return Response.ok("Done").build();
    }






}
