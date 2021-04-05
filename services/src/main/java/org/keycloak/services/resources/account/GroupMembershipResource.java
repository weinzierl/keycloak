package org.keycloak.services.resources.account;

import org.jboss.resteasy.annotations.cache.NoCache;
import org.jboss.resteasy.spi.HttpRequest;
import org.keycloak.common.util.KeycloakUriBuilder;
import org.keycloak.email.EmailException;
import org.keycloak.email.EmailTemplateProvider;
import org.keycloak.events.EventBuilder;
import org.keycloak.models.*;
import org.keycloak.models.utils.ModelToRepresentation;
import org.keycloak.representations.idm.GroupRepresentation;
import org.keycloak.representations.idm.JoinGroupRequestRepresentation;
import org.keycloak.services.ServicesLogger;
import org.keycloak.services.Urls;
import org.keycloak.services.managers.Auth;
import org.keycloak.services.resources.RealmsResource;
import org.keycloak.urls.UrlType;

import javax.ws.rs.*;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class GroupMembershipResource {

    private final KeycloakSession session;
    private final EventBuilder event;
    private final UserModel user;
    private final Auth auth;
    private final RealmModel realm;
    private final KeycloakUriInfo uriInfo;

    public GroupMembershipResource(KeycloakSession session,
                                   Auth auth,
                                   EventBuilder event,
                                   UserModel user) {
        this.session = session;
        this.auth = auth;
        this.event = event;
        this.user = user;
        this.realm = session.getContext().getRealm();
        this.uriInfo = session.getContext().getUri();
    }

    @GET
    @Path("/")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Stream<GroupRepresentation> groupMembership(@QueryParam("briefRepresentation") @DefaultValue("true") boolean briefRepresentation) {
        auth.require(AccountRoles.VIEW_GROUPS);

         return ModelToRepresentation.toGroupHierarchy(user, !briefRepresentation);
    }

    @GET
    @Path("/all")
    @NoCache
    @Produces(MediaType.APPLICATION_JSON)
    public Response getGroups(@QueryParam("search") @DefaultValue("") String search,
                              @QueryParam("first") @DefaultValue("0") Integer firstResult,
                              @QueryParam("max") @DefaultValue("10") Integer maxResults) {
        auth.require(AccountRoles.VIEW_GROUPS);

        return Response.ok().entity(ModelToRepresentation.searchForAllGroupByName(realm, search.trim(), firstResult, maxResults).collect(Collectors.toList())).links(createPageLinks(firstResult, maxResults, realm.getGroupsCountByNameContaining(search.trim()).intValue())).build();

        //return ModelToRepresentation.toGroupHierarchy(realm, true);

    }
    
    @POST
    @Path("/join")
    @Consumes(MediaType.APPLICATION_JSON)
    public void requestJoinGroup(JoinGroupRequestRepresentation rep) {
        auth.require(AccountRoles.VIEW_GROUPS);

        try {
            String link = Urls.userAdminConsoleURi(session.getContext().getUri(UrlType.FRONTEND).getBaseUri(), realm.getName(),user.getId()).replace("%23","#");
            session.getProvider(EmailTemplateProvider.class).setRealm(realm).setUser(user).sendJoinGroupRequestEmail(link);
        } catch (EmailException e) {
            ServicesLogger.LOGGER.failedToSendEmail(e);
        }

    }
    
    private Link[] createPageLinks(Integer first, Integer max, Integer resultSize) {
        if (resultSize == 0 || (first == 0 && resultSize <= max)) {
            return new Link[] {};
        }

        List<Link> links = new ArrayList();
        boolean nextPage = resultSize > (first + max);

        if (nextPage) {
            links.add(Link.fromUri(KeycloakUriBuilder.fromUri(uriInfo.getRequestUri()).replaceQuery("first={first}&max={max}")
                .build(first + max, max)).rel("next").build());
        }

        if (first > 0) {
            links.add(Link.fromUri(KeycloakUriBuilder.fromUri(uriInfo.getRequestUri()).replaceQuery("first={first}&max={max}")
                .build(Math.max(first - max, 0), max)).rel("prev").build());
        }

        return links.toArray(new Link[links.size()]);
    }

}
