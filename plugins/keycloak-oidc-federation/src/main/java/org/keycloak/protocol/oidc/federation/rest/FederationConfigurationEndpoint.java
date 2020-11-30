package org.keycloak.protocol.oidc.federation.rest;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.keycloak.jose.jws.JWSInput;
import org.keycloak.jose.jws.JWSInputException;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.protocol.oidc.federation.model.AuthorityHint;
import org.keycloak.protocol.oidc.federation.model.AuthorityHintService;
import org.keycloak.protocol.oidc.federation.model.TrustAnchor;
import org.keycloak.protocol.oidc.federation.model.TrustAnchorService;
import org.keycloak.representations.AccessToken;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.managers.AppAuthManager;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;
import org.keycloak.services.resources.admin.AdminAuth;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;
import org.keycloak.services.resources.admin.permissions.AdminPermissions;

public class FederationConfigurationEndpoint {

    private static final Logger logger = Logger.getLogger(FederationConfigurationEndpoint.class);

    private KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AuthorityHintService authorityHintService;
    private TrustAnchorService trustAnchorService;

    public FederationConfigurationEndpoint(KeycloakSession session) {
        this.session = session;
        this.auth = authenticateRealmAdminRequest();
        this.authorityHintService = new AuthorityHintService(session);
        this.trustAnchorService = new TrustAnchorService(session);
    }

    private AdminPermissionEvaluator authenticateRealmAdminRequest() {
        AppAuthManager authManager = new AppAuthManager();
        HttpHeaders headers = session.getContext().getRequestHeaders();
        String tokenString = authManager.extractAuthorizationHeaderToken(headers);
        if (tokenString == null) {
            throw new NotAuthorizedException("Bearer");
        }
        AccessToken token;
        try {
            JWSInput input = new JWSInput(tokenString);
            token = input.readJsonContent(AccessToken.class);
        } catch (JWSInputException e) {
            throw new NotAuthorizedException("Bearer token format error");
        }
        String realmName = token.getIssuer().substring(token.getIssuer().lastIndexOf('/') + 1);
        RealmManager realmManager = new RealmManager(session);
        RealmModel realm = realmManager.getRealmByName(realmName);
        if (realm == null) {
            throw new NotAuthorizedException("Unknown realm in token");
        } 
        if ( !realm.getId().equals( session.getContext().getRealm().getId())) {
            throw new NotAuthorizedException("False realm in token");
        } 
        AuthenticationManager.AuthResult authResult = authManager.authenticateBearerToken(session, realm, session.getContext().getUri(), this.session.getContext().getConnection(), headers);
        if (authResult == null) {
            logger.debug("Token not valid");
            throw new NotAuthorizedException("Bearer");
        }

        ClientModel client = realm.getClientByClientId(token.getIssuedFor());
        if (client == null) {
            throw new NotFoundException("Could not find client for authorization");

        }

        AdminAuth adminAuth = new AdminAuth(realm, authResult.getToken(), authResult.getUser(), client);
        return AdminPermissions.evaluator(session, realm, adminAuth);
    }

    @GET
    @Path("authority-hint")
    @Produces(MediaType.APPLICATION_JSON)
    public List<AuthorityHint> getRealmAuthorityHint() {
        this.auth.realm().requireViewRealm();
        List<AuthorityHint> results = authorityHintService.findAuthorityHintsByRealm();
        return results;
    }

    @POST
    @Path("authority-hint")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRealmAuthorityHint(String value) {
        this.auth.realm().requireManageRealm();
        try {
            AuthorityHint authHint = authorityHintService.create(value);
            return Response.ok(authHint).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

    }

    @DELETE
    @Path("authority-hint/{id}")
    public Response deleteAuthorityHint(@PathParam("id") String id) {
        this.auth.realm().requireManageRealm();
        try {
            authorityHintService.delete(id);
        } catch (NotFoundException e) {
            e.printStackTrace();
            return ErrorResponse.error(e.getMessage(), Response.Status.NOT_FOUND);
        }
        return Response.ok("Authority hint was deleted").build();
    }

    @GET
    @Path("trust-anchor")
    @Produces(MediaType.APPLICATION_JSON)
    public List<TrustAnchor> getRealmTrustAnchor() {
        this.auth.realm().requireViewRealm();
        List<TrustAnchor> results = trustAnchorService.findTrustAnchorByRealm();
        return results;
    }

    @POST
    @Path("trust-anchor")
    @Consumes(MediaType.TEXT_PLAIN)
    @Produces(MediaType.APPLICATION_JSON)
    public Response createRealmTrustAnchor(String value) {
        this.auth.realm().requireManageRealm();
        try {
            TrustAnchor anchor = trustAnchorService.create(value);
            return Response.ok(anchor).build();
        } catch (Exception e) {
            e.printStackTrace();
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

    }

    @DELETE
    @Path("trust-anchor/{id}")
    public Response deleteTrustAnchor(@PathParam("id") String id) {
        this.auth.realm().requireManageRealm();
        try {
            trustAnchorService.delete(id);
        } catch (NotFoundException e) {
            e.printStackTrace();
            return ErrorResponse.error(e.getMessage(), Response.Status.NOT_FOUND);
        }
        return Response.ok("TrustAnchor  was deleted").build();
    }
    
    //DUMMY endpoint for start configuration
    @POST
    @Path("default")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response startConfiguration(FederationConf conf) {
        this.auth.realm().requireManageRealm();
        try {
            conf.getAuthHints().stream().forEach(val -> authorityHintService.create(val));
            conf.getTrustAnchors().stream().forEach(val -> trustAnchorService.create(val));
            return Response.ok("Configuration success").build();
        } catch (Exception e) {
            e.printStackTrace();
            return ErrorResponse.error(e.getMessage(), Response.Status.BAD_REQUEST);
        }

    }
    
    public static class FederationConf {
        private List<String> authHints;
        private List<String> trustAnchors;
                
        public List<String> getAuthHints() {
            return authHints;
        }
        public void setAuthHints(List<String> authHints) {
            this.authHints = authHints;
        }
        public List<String> getTrustAnchors() {
            return trustAnchors;
        }
        public void setTrustAnchors(List<String> trustAnchors) {
            this.trustAnchors = trustAnchors;
        }
        
        
    }

}
