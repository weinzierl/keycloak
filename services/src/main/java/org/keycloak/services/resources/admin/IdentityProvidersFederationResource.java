/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.keycloak.services.resources.admin;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.broker.federation.IdpFederationProviderFactory;
import org.keycloak.broker.provider.IdentityProviderFactory;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelDuplicateException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;
import org.keycloak.services.ErrorResponse;
import org.keycloak.services.resources.admin.permissions.AdminPermissionEvaluator;


public class IdentityProvidersFederationResource {

    private final RealmModel realm;
    private final KeycloakSession session;
    private AdminPermissionEvaluator auth;
    private AdminEventBuilder adminEvent;

    public IdentityProvidersFederationResource(RealmModel realm, KeycloakSession session, AdminPermissionEvaluator auth, AdminEventBuilder adminEvent) {
        this.realm = realm;
        this.session = session;
        this.auth = auth;
        this.adminEvent = adminEvent.resource(ResourceType.IDENTITY_PROVIDERS_FEDERATION);
    }


    /**
     * Import identity provider from JSON body
     *
     * @param data JSON body
     * @return
     * @throws IOException
     */
    @POST
    @Path("import-config")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Set<String> importFrom(Map<String, Object> data) throws IOException {
        this.auth.realm().requireManageIdentityProviders();
        if (!(data.containsKey("providerId") && data.containsKey("fromUrl"))) {
            throw new BadRequestException();
        }
        String providerId = data.get("providerId").toString();
        String from = data.get("fromUrl").toString();
        InputStream inputStream = session.getProvider(HttpClientProvider.class).get(from);
        
		try {
			IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, providerId).create(session, new IdentityProvidersFederationModel(),this.realm.getId());
			return idpFederationProvider.parseIdps(session, inputStream);
		} finally {
			try {
				inputStream.close();
			} catch (IOException e) {
			}
		}
        
    }

    /**
     * Export public broker configuration for identity provider
     *
     * @param format Format to use
     * @return
     */
    @GET
    @Path("instances/{alias}/export")
    @NoCache
    public Response export(
    		@PathParam("alias") String alias
    		) {
        this.auth.realm().requireViewIdentityProviders();

        IdentityProvidersFederationModel model = realm.getIdentityProvidersFederationByAlias(alias);
        
        if (model == null)
            throw new javax.ws.rs.NotFoundException();

        try {
        	IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session, model, this.realm.getId());
        	return idpFederationProvider.export(session.getContext().getUri(), realm);
        } catch (Exception e) {
            return ErrorResponse.error("Could not export public broker configuration for IdP aggregation [" + model.getProviderId() + "].", Response.Status.NOT_FOUND);
        }
        
    }
    
    
    /**
     * Get a list with all identity provider federations of the realm
     *
     * @return
     */
    @GET
    @Path("instances")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public List<IdentityProvidersFederationRepresentation> list() {
        this.auth.realm().requireViewIdentityProviders();
        return realm.getIdentityProviderFederations().stream().map(model -> ModelToRepresentation.toRepresentation(model)).collect(Collectors.toList());
    }
    
    /**
     * Create a new identity provider federation
     *
     * @param representation JSON body
     * @return
     */
    @POST
    @Path("instances")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response create(IdentityProvidersFederationRepresentation representation) {
        this.auth.realm().requireManageIdentityProviders();

        try {
        	IdentityProvidersFederationModel model = RepresentationToModel.toModel(representation);
        	IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session,model,this.realm.getId());
        	String id = idpFederationProvider.updateFederation();
        	
			adminEvent.operation(OperationType.CREATE)
					.resourcePath(session.getContext().getUri(), representation.getAlias())
					.representation(representation).success();
			idpFederationProvider.enableUpdateTask();
			
			return Response.created(session.getContext().getUri().getAbsolutePathBuilder()
					.path(id).build()).build();
        	 
        } catch (ModelDuplicateException e) {
            return ErrorResponse.exists("Identity Provider federation" + representation.getAlias() + " already exists");
        }
    }
    
    @GET
    @Path("instances/{id}")
    @Produces(MediaType.APPLICATION_JSON)
    public IdentityProvidersFederationRepresentation getIdentityProviderFederation(@PathParam("id") String internalId) {
        this.auth.realm().requireViewIdentityProviders();
        //create alias representation
        IdentityProvidersFederationModel model = realm.getIdentityProvidersFederationById(internalId);
        if (model == null)
        	throw new NotFoundException();
        IdentityProvidersFederationRepresentation representation = ModelToRepresentation.toRepresentation(model);
        return representation;
    }

    /**
     * Delete the identity provider federation, along with all its IdPs
     *
     * @return
     */
    @DELETE
    @Path("instances/{id}")
    @Consumes(MediaType.APPLICATION_JSON)
    @NoCache
    public Response delete(@PathParam("id") String internalId) {
        this.auth.realm().requireManageIdentityProviders();
        
        IdentityProvidersFederationModel model = realm.getIdentityProvidersFederationById(internalId);
        if (model == null)
        	throw new NotFoundException();
    	IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session,model,this.realm.getId());
        
    	idpFederationProvider.removeFederation();
        
        adminEvent.operation(OperationType.DELETE).resourcePath(session.getContext().getUri()).success();
        return Response.noContent().build();
    }

}