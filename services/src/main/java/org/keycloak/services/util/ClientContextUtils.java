package org.keycloak.services.util;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.keycloak.common.Profile;
import org.keycloak.models.ClientModel;
import org.keycloak.models.ClientScopeModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ProtocolMapperModel;
import org.keycloak.models.RoleModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.utils.RoleUtils;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.oidc.TokenManager;
import org.keycloak.protocol.oidc.rar.model.IntermediaryScopeRepresentation;

public class ClientContextUtils {

    private final ClientModel client;
    private final String tokenScope;
    private final UserModel user;
    private final KeycloakSession session;
    private Set<ClientScopeModel> clientScopes;
    private Set<ProtocolMapperModel> protocolMappers;
    private Set<RoleModel> userRoles;

    public ClientContextUtils(ClientModel client, String tokenScope, KeycloakSession session, UserModel user) {
        this.client = client;
        this.tokenScope = tokenScope;
        this.session = session;
        this.user = user;
    }

    public Stream<ClientScopeModel> getClientScopesStream() {
        // Load client scopes if not yet present
        if (clientScopes == null) {
            clientScopes = loadClientScopes();
        }
        return clientScopes.stream();
    }

    private Set<ClientScopeModel> loadClientScopes() {
        return this.getRequestedClientScopes(tokenScope, client).filter(this::isClientScopePermittedForUser).collect(Collectors.toSet());
    }

    public static Stream<ClientScopeModel> getRequestedClientScopes(String scopeParam, ClientModel client) {
        // Add all default client scopes automatically and client itself
        Stream<ClientScopeModel> clientScopes = Stream.concat(
                client.getClientScopes(true).values().stream(),
                Stream.of(client)).distinct();

        if (scopeParam == null) {
            return clientScopes;
        }

        Map<String, ClientScopeModel> allOptionalScopes = client.getClientScopes(false);
        // Add optional client scopes requested by scope parameter
        if (Profile.isFeatureEnabled(Profile.Feature.DYNAMIC_SCOPES)) {
            List<String> scopeList = Arrays.asList(scopeParam.split(" "));
            return Stream.concat(allOptionalScopes.values().stream().filter(sp -> scopeList.contains(sp.getName()) || (sp.isDynamicScope() && scopeList.stream().anyMatch(Pattern.compile(sp.getDynamicScopeRegexp().replace(":*", ":(.*)")).asPredicate()))),
                    clientScopes).distinct();
        } else {
            return Stream.concat(TokenManager.parseScopeParameter(scopeParam).map(allOptionalScopes::get).filter(Objects::nonNull),
                    clientScopes).distinct();
        }
    }

    private boolean isClientScopePermittedForUser(ClientScopeModel clientScope) {
        if (clientScope instanceof ClientModel) {
            return true;
        }

        Set<RoleModel> clientScopeRoles = clientScope.getScopeMappingsStream().collect(Collectors.toSet());

        // Client scope is automatically permitted if it doesn't have any role scope mappings
        if (clientScopeRoles.isEmpty()) {
            return true;
        }

        // Expand (resolve composite roles)
        clientScopeRoles = RoleUtils.expandCompositeRoles(clientScopeRoles);

        // Check if expanded roles of clientScope has any intersection with expanded roles of user. If not, it is not permitted
        clientScopeRoles.retainAll(getUserRoles());
        return !clientScopeRoles.isEmpty();
    }

    private Set<RoleModel> getUserRoles() {
        // Load userRoles if not yet present
        if (userRoles == null) {
            userRoles = RoleUtils.getDeepUserRoleMappings(user);
        }
        return userRoles;
    }

    public Stream<ProtocolMapperModel> getProtocolMappersStream() {
        // Load protocolMappers if not yet present
        if (protocolMappers == null) {
            protocolMappers = loadProtocolMappers();
        }
        return protocolMappers.stream();
    }

    private Set<ProtocolMapperModel> loadProtocolMappers() {

        String finalProtocol = client.getProtocol();
        return getClientScopesStream()
                .flatMap(clientScope -> clientScope.getProtocolMappersStream()
                        .filter(mapper -> Objects.equals(finalProtocol, mapper.getProtocol()) &&
                                ProtocolMapperUtils.isEnabled(session, mapper)))
                .collect(Collectors.toSet());
    }

}
