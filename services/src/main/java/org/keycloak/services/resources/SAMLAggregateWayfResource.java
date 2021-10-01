package org.keycloak.services.resources;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
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
import javax.ws.rs.core.UriBuilder;

import org.jboss.logging.Logger;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.OAuth2Constants;
import org.keycloak.broker.saml.aggregate.SAMLAggregateIdentityProviderFactory;
import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateMetadataStoreProvider;
import org.keycloak.broker.saml.aggregate.metadata.SAMLIdpDescriptor;
import org.keycloak.common.ClientConnection;
import org.keycloak.common.util.Base64Url;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.events.Errors;
import org.keycloak.events.EventBuilder;
import org.keycloak.events.EventType;
import org.keycloak.forms.login.LoginFormsProvider;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.protocol.oidc.utils.PkceUtils;
import org.keycloak.representations.AccessToken;
import org.keycloak.representations.saml.SAMLAggreateWayfResponseRepresentation;
import org.keycloak.representations.saml.SAMLAggregateIdpRepresentation;
import org.keycloak.services.ErrorResponseException;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.services.managers.RealmManager;

import com.google.common.base.Strings;
import org.keycloak.theme.FreeMarkerException;

@Path("/realms")
public class SAMLAggregateWayfResource {

  protected static final Logger LOG = Logger.getLogger(SAMLAggregateWayfResource.class);

  private final String PKCE_METHOD = "S256";

  @Context
  protected KeycloakSession session;

  @Context
  protected ClientConnection clientConnection;

  @Context
  private HttpRequest request;

  private RealmModel init(String realmName) {
    RealmManager realmManager = new RealmManager(session);
    RealmModel realmModel = realmManager.getRealmByName(realmName);
    if (realmModel == null) {
      throw new NotFoundException("Realm does not exist");
    }
    session.getContext().setRealm(realmModel);
    return realmModel;
  }

  @GET
  @Path("{realm}/saml-wayf-page")
  @Produces(MediaType.TEXT_HTML)
  public Response getWayfPage(final @PathParam("realm") String name,
                              @QueryParam("provider") String providerAlias,
                              @QueryParam("clientId") String clientId) {

    if (Strings.isNullOrEmpty(providerAlias)) {
      throw new ErrorResponseException("Bad request", "Please specify a provider",
              Response.Status.BAD_REQUEST);
    }

    RealmModel realmModel = init(name);
    IdentityProviderModel idpConfig = realmModel.getIdentityProviderByAlias(providerAlias);

    if (Objects.isNull(idpConfig) || !SAMLAggregateIdentityProviderFactory.PROVIDER_ID.equals(idpConfig.getProviderId())) {
      throw new ErrorResponseException("Invalid WAYF provider",
              "Provider " + providerAlias + " does not exist or is not a SAMLAggregateProvider",
              Response.Status.BAD_REQUEST);
    }

    String BASE_URL = request.getUri().getBaseUri().toString();
    String actionUrl;

    LoginFormsProvider loginFormsProvider = session.getProvider(LoginFormsProvider.class);
    SAMLAggregateMetadataStoreProvider md = session.getProvider(SAMLAggregateMetadataStoreProvider.class);
    List<SAMLIdpDescriptor> descriptors = md.getEntities(realmModel, providerAlias);

    AuthenticationManager.AuthResult cookieResult = AuthenticationManager.authenticateIdentityCookie(session, realmModel, true);

    if (cookieResult == null) {
      // not logged in => LOGIN

      actionUrl = BASE_URL + "realms/" + name + "/protocol/openid-connect/auth";

      String state =  UUID.randomUUID().toString();
      String responseType = "code";

      String codeVerifier = PkceUtils.generateCodeVerifier();
      String codeChallenge = PkceUtils.encodeCodeChallenge(codeVerifier, PKCE_METHOD);


      loginFormsProvider.setAttribute("isLogin", true);
      loginFormsProvider.setAttribute("isLinking", false);
      loginFormsProvider.setAttribute("provider", providerAlias);
      loginFormsProvider.setAttribute("descriptors", descriptors);
      loginFormsProvider.setAttribute("actionUrl", actionUrl);

      loginFormsProvider.setAttribute("code_challenge", codeChallenge);
      loginFormsProvider.setAttribute("code_challenge_method", PKCE_METHOD);
      loginFormsProvider.setAttribute("clientId", clientId);
      loginFormsProvider.setAttribute("state", state);
      loginFormsProvider.setAttribute("responseType", responseType);
      loginFormsProvider.setAttribute("sessionCode", Base64Url.encode(KeycloakModelUtils.generateSecret()));

    } else {
      // not logged in => IDENTITY LINKING
      actionUrl = BASE_URL + "realms/" + name + "/account/identity";

      AccessToken token = cookieResult.getToken();
      String nonce = UUID.randomUUID().toString();
      MessageDigest messageDigest = null;
      try {
        messageDigest = MessageDigest.getInstance("SHA-256");
      } catch (NoSuchAlgorithmException e) {
        throw new RuntimeException(e);
      }
      String input = nonce + token.getSessionState() + clientId + providerAlias;
      byte[] check = messageDigest.digest(input.getBytes(StandardCharsets.UTF_8));
      String hash = Base64Url.encode(check);
      String redirectUri = BASE_URL + "realms/" + name + "/account";
      String accountLinkUrl = KeycloakUriBuilder.fromUri(BASE_URL)
              .path("/realms/" + name + "/broker/" + providerAlias + "/link")
              .queryParam("nonce", nonce)
              .queryParam("hash", hash)
              .queryParam("client_id", clientId)
              .queryParam("redirect_uri", redirectUri).build(realmModel, providerAlias).toString();


      String state =  UUID.randomUUID().toString();
      String responseType = "code";

      loginFormsProvider.setAttribute("isLinking", true);
      loginFormsProvider.setAttribute("isLogin", false);
      loginFormsProvider.setAttribute("provider", providerAlias);
      loginFormsProvider.setAttribute("descriptors", descriptors);
      loginFormsProvider.setAttribute("actionUrl", accountLinkUrl);
      loginFormsProvider.setAttribute("state", state);

      loginFormsProvider.setAttribute("nonce", nonce);
      loginFormsProvider.setAttribute("hash", hash);
      loginFormsProvider.setAttribute("client_id", clientId);
      loginFormsProvider.setAttribute("redirect_uri", redirectUri);

      loginFormsProvider.setAttribute("sessionCode", Base64Url.encode(KeycloakModelUtils.generateSecret()));
    }

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
