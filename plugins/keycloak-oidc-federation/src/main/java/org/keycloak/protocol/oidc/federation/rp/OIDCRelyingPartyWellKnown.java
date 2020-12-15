package org.keycloak.protocol.oidc.federation.rp;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.logging.Logger;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;

public class OIDCRelyingPartyWellKnown {

    private static final Logger logger = Logger.getLogger(OIDCRelyingPartyWellKnown.class);
    
    private KeycloakSession session;
    private String idpAlias;
    
    
    public OIDCRelyingPartyWellKnown(KeycloakSession session, String idpAlias) {
        this.session = session;
        this.idpAlias = idpAlias;
    }
    
    
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public String getConfiguration() {
        RealmModel realm = session.getContext().getRealm();
        IdentityProviderModel idpModel = realm.getIdentityProviderByAlias(idpAlias);
        //form here the response of the well-known
        
        
        
        
        return String.format("{\"status\": \"Success\", \"realm\": \"%s\"}", realm.getName());
    }
    
    
    
    
    
    
}
