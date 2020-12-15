package org.keycloak.protocol.oidc.federation.rp;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.services.resource.RealmResourceProvider;

public class OIDCRelyingPartyResourceProvider implements RealmResourceProvider {
    
    private KeycloakSession session;

    public OIDCRelyingPartyResourceProvider(KeycloakSession session) {
        this.session = session;
    }
    
    @Override
    public void close() {
        
    }

    @Override
    public Object getResource() {
        return this;
    }
    
    
    @Path("{idpAlias}/.well-known/oidc-federation")
    public OIDCRelyingPartyWellKnown getWellKnownService(@PathParam("idpAlias") String idpAlias) {
        OIDCRelyingPartyWellKnown rpWellKnown = new OIDCRelyingPartyWellKnown(session, idpAlias);
        ResteasyProviderFactory.getInstance().injectProperties(rpWellKnown);
        return rpWellKnown;
    }
    
    

}
