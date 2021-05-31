package org.keycloak.services.resources;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.ws.rs.GET;
import javax.ws.rs.NotFoundException;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.broker.saml.aggregate.SAMLAggregateIdentityProviderFactory;
import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateMetadataStoreProvider;
import org.keycloak.broker.saml.aggregate.metadata.SAMLIdpDescriptor;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Base64Url;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.representations.saml.SAMLAggreateWayfResponseRepresentation;
import org.keycloak.representations.saml.SAMLAggregateIdpRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.RealmManager;

import com.google.common.base.Strings;
import org.keycloak.theme.FreeMarkerException;

@Path("/realms")
public class SAMLAggregateWayfResource {

  protected static final Logger LOG = Logger.getLogger(SAMLAggregateWayfResource.class);

  @Context
  protected KeycloakSession session;

  @Context
  protected ClientConnection clientConnection;

  @Context
  private HttpRequest request;

  private RealmModel init(String realmName) {
    RealmManager realmManager = new RealmManager(session);
    RealmModel realm = realmManager.getRealmByName(realmName);
    if (realm == null) {
      throw new NotFoundException("Realm does not exist");
    }
    session.getContext().setRealm(realm);
    return realm;
  }

  @GET
  @Path("{realm}/saml-wayf-page")
  @Produces(MediaType.TEXT_HTML)
  public Response getWayfPage(final @PathParam("realm") String name,
                              @QueryParam("provider") String providerAlias,
                              @QueryParam("sessionCode") String sessionCode,
                              @QueryParam("clientId") String clientId) throws IOException, FreeMarkerException {

    if (Strings.isNullOrEmpty(providerAlias)) {
      throw new ErrorResponseException("Bad request", "Please specify a provider",
              Response.Status.BAD_REQUEST);
    }


    RealmModel realm = init(name);

    IdentityProviderModel idpConfig = realm.getIdentityProviderByAlias(providerAlias);

    if (Objects.isNull(idpConfig) || !SAMLAggregateIdentityProviderFactory.PROVIDER_ID.equals(idpConfig.getProviderId())) {
      throw new ErrorResponseException("Invalid WAYF provider",
              "Provider " + providerAlias + " does not exist or is not a SAMLAggregateProvider",
              Response.Status.BAD_REQUEST);
    }

    //final String BASE_URL = "http://dev.local.io:8081/auth/realms/";
    String BASE_URL = request.getUri().getBaseUri().toString();
    String redirectUri = BASE_URL + "realms/" + name + "/account/login-redirect";
    String state =  UUID.randomUUID().toString();
    String actionUrl = BASE_URL + "realms/" + name + "/protocol/openid-connect/auth";
    String responseType = "code";

    SAMLAggregateMetadataStoreProvider md =
            session.getProvider(SAMLAggregateMetadataStoreProvider.class);
    List<SAMLIdpDescriptor> descriptors = md.getEntities(realm, providerAlias);

    LoginFormsProvider loginFormsProvider = session.getProvider(LoginFormsProvider.class);
    loginFormsProvider.setAttribute("provider", providerAlias);
    loginFormsProvider.setAttribute("descriptors", descriptors);
    loginFormsProvider.setAttribute("actionUrl", actionUrl);

    loginFormsProvider.setAttribute("clientId", clientId);
    loginFormsProvider.setAttribute("redirectUri", redirectUri);
    loginFormsProvider.setAttribute("state", state);
    loginFormsProvider.setAttribute("responseType", responseType);
    loginFormsProvider.setAttribute("sessionCode", Base64Url.encode(KeycloakModelUtils.generateSecret()));

    return loginFormsProvider.createSamlWayf();
  }

  @GET
  @Path("{realm}/saml-wayf")
  @Produces(MediaType.APPLICATION_JSON)
  public Response lookupIdps(final @PathParam("realm") String name,
      @QueryParam("provider") String providerAlias, @QueryParam("q") String matchString) {

    if (Strings.isNullOrEmpty(providerAlias)) {
      throw new ErrorResponseException("Bad request", "Please specify a provider",
          Response.Status.BAD_REQUEST);
    }

    if (Strings.isNullOrEmpty(matchString)) {
      throw new ErrorResponseException("Bad request", "Please specify a query string",
          Response.Status.BAD_REQUEST);
    }

    RealmModel realm = init(name);

    IdentityProviderModel idpConfig = realm.getIdentityProviderByAlias(providerAlias);

    if (!SAMLAggregateIdentityProviderFactory.PROVIDER_ID.equals(idpConfig.getProviderId())) {
      throw new ErrorResponseException("Invalid WAYF provider",
          "Provider " + providerAlias + " does not exist or is not a SAMLAggregateProvider",
          Response.Status.BAD_REQUEST);
    }

    SAMLAggregateMetadataStoreProvider md =
        session.getProvider(SAMLAggregateMetadataStoreProvider.class);

    List<SAMLAggregateIdpRepresentation> results =
        md.lookupEntities(realm, providerAlias, matchString)
          .stream()
          .map(this::toRepresentation)
          .collect(Collectors.toList());

    SAMLAggreateWayfResponseRepresentation envelope = new SAMLAggreateWayfResponseRepresentation();

    envelope.setProvider(providerAlias);
    envelope.setQuery(matchString);
    envelope.setRealm(realm.getName());
    envelope.setResults(results);

    return Response.ok(envelope).build();
  }



  SAMLAggregateIdpRepresentation toRepresentation(SAMLIdpDescriptor descriptor) {
    SAMLAggregateIdpRepresentation repr = new SAMLAggregateIdpRepresentation();
    repr.setDiplayName(descriptor.getDisplayName());
    repr.setEntityId(descriptor.getEntityId());
    return repr;
  }

}
