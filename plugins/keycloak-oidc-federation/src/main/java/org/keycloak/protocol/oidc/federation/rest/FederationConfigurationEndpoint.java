package org.keycloak.protocol.oidc.federation.rest;


import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAuthorizedException;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
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
import org.keycloak.protocol.oidc.federation.model.Configuration;
import org.keycloak.protocol.oidc.federation.model.ConfigurationEntity;
import org.keycloak.protocol.oidc.federation.model.ConfigurationService;
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
    private ConfigurationService configurationService;

    public FederationConfigurationEndpoint(KeycloakSession session) {
        this.session = session;
        this.auth = authenticateRealmAdminRequest();
        this.configurationService = new ConfigurationService(session);
        
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
    @Produces(MediaType.APPLICATION_JSON)
    public Configuration getConfiguration() {
        this.auth.realm().requireViewRealm();
        ConfigurationEntity entity = configurationService.getEntity();
        return entity != null ? entity.getConfiguration() : null;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response saveConfiguration(Configuration configuration) {
        this.auth.realm().requireManageRealm();
        RealmModel realmModel = session.getContext().getRealm();
        try {
            ConfigurationEntity entity = configurationService.getEntity();
            if(entity == null)
                entity = new ConfigurationEntity(realmModel.getId(), configuration);
            else
                entity.setConfiguration(configuration);
            configurationService.saveEntity(entity);
            return Response.ok(configuration).build();
        }
        catch(Exception ex) {
            return ErrorResponse.error("Failed to create configuration for the realm " + realmModel.getName(), Response.Status.NOT_FOUND);
        }
    }

    @DELETE
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteConfiguration() {
        this.auth.realm().requireManageRealm();
        try {
            configurationService.deleteEntity();
        } catch (NotFoundException e) {
            e.printStackTrace();
            return ErrorResponse.error(e.getMessage(), Response.Status.NOT_FOUND);
        }
        return Response.ok("Authority hint was deleted").build();
    }

    
}
