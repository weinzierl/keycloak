/*
 * Copyright 2022 Red Hat, Inc. and/or its affiliates
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

package org.keycloak.models.map.realm;

import org.keycloak.common.util.Time;
import org.keycloak.models.map.annotations.GenerateEntityImplementations;
import org.keycloak.models.map.annotations.IgnoreForEntityImplementationGenerator;
import org.keycloak.models.map.common.AbstractEntity;
import org.keycloak.models.map.common.DeepCloner;
import org.keycloak.models.map.common.EntityWithAttributes;
import org.keycloak.models.map.common.UpdatableEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationExecutionEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticationFlowEntity;
import org.keycloak.models.map.realm.entity.MapAuthenticatorConfigEntity;
import org.keycloak.models.map.realm.entity.MapClientInitialAccessEntity;
import org.keycloak.models.map.realm.entity.MapComponentEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderEntity;
import org.keycloak.models.map.realm.entity.MapIdentityProviderMapperEntity;
import org.keycloak.models.map.realm.entity.MapOTPPolicyEntity;
import org.keycloak.models.map.realm.entity.MapRequiredActionProviderEntity;
import org.keycloak.models.map.realm.entity.MapRequiredCredentialEntity;
import org.keycloak.models.map.realm.entity.MapWebAuthnPolicyEntity;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@GenerateEntityImplementations(
        inherits = "org.keycloak.models.map.realm.MapRealmEntity.AbstractRealmEntity"
)
@DeepCloner.Root
public interface MapRealmEntity extends UpdatableEntity, AbstractEntity, EntityWithAttributes {

    public abstract class AbstractRealmEntity extends UpdatableEntity.Impl implements MapRealmEntity {
    private String id;
//    private String name;
//
//private Boolean enabled = false;
//    private Boolean registrationAllowed = false;
//    private Boolean registrationEmailAsUsername = false;
//    private Boolean verifyEmail = false;
//    private Boolean resetPasswordAllowed = false;
//    private Boolean loginWithEmailAllowed = false;
//    private Boolean duplicateEmailsAllowed = false;
//    private Boolean rememberMe = false;
//    private Boolean editUsernameAllowed = false;
//    private Boolean revokeRefreshToken = false;
//    private Boolean adminEventsEnabled = false;
//    private Boolean adminEventsDetailsEnabled = false;
//    private Boolean internationalizationEnabled = false;
//    private Boolean allowUserManagedAccess = false;
//    private Boolean offlineSessionMaxLifespanEnabled = false;
//    private Boolean eventsEnabled = false;
//    private Integer refreshTokenMaxReuse = 0;
//    private Integer ssoSessionIdleTimeout = 0;
//    private Integer ssoSessionMaxLifespan = 0;
//    private Integer ssoSessionIdleTimeoutRememberMe = 0;
//    private Integer ssoSessionMaxLifespanRememberMe = 0;
//    private Integer offlineSessionIdleTimeout = 0;
//    private Integer accessTokenLifespan = 0;
//    private Integer accessTokenLifespanForImplicitFlow = 0;
//    private Integer accessCodeLifespan = 0;
//    private Integer accessCodeLifespanUserAction = 0;
//    private Integer accessCodeLifespanLogin = 0;
//    private Integer notBefore = 0;
//    private Integer clientSessionIdleTimeout = 0;
//    private Integer clientSessionMaxLifespan = 0;
//    private Integer clientOfflineSessionIdleTimeout = 0;
//    private Integer clientOfflineSessionMaxLifespan = 0;
//    private Integer actionTokenGeneratedByAdminLifespan = 0;
//    private Integer offlineSessionMaxLifespan = 0;
//    private Long eventsExpiration = 0l;
//    private String displayName;
//    private String displayNameHtml;
//    private String passwordPolicy;
//    private String sslRequired;
//    private String loginTheme;
//    private String accountTheme;
//    private String adminTheme;
//    private String emailTheme;
//    private String masterAdminClient;
//    private String defaultRoleId;
//    private String defaultLocale;
//    private String browserFlow;
//    private String registrationFlow;
//    private String directGrantFlow;
//    private String resetCredentialsFlow;
//    private String clientAuthenticationFlow;
//    private String dockerAuthenticationFlow;
//    private MapOTPPolicyEntity otpPolicy = MapOTPPolicyEntity.fromModel(OTPPolicy.DEFAULT_POLICY);;
//    private MapWebAuthnPolicyEntity webAuthnPolicy = MapWebAuthnPolicyEntity.defaultWebAuthnPolicy();;
//    private MapWebAuthnPolicyEntity webAuthnPolicyPasswordless = MapWebAuthnPolicyEntity.defaultWebAuthnPolicy();;
//
//
//    private Set<String> eventsListeners = new HashSet<>();
//    private Set<String> enabledEventTypes = new HashSet<>();
//    private Set<String> supportedLocales = new HashSet<>();
//    private Map<String, String> browserSecurityHeaders = new HashMap<>();
//    private Map<String, String> smtpConfig = new HashMap<>();
//
//    private final Set<String> defaultGroupIds = new HashSet<>();
//    private final Set<String> defaultClientScopes = new HashSet<>();
//    private final Set<String> optionalClientScopes = new HashSet<>();
//    private final Map<String, List<String>> attributes = new HashMap<>();
//    private final Map<String, Map<String, String>> localizationTexts = new HashMap<>();
//    private final Map<String, MapClientInitialAccessEntity> clientInitialAccesses = new HashMap<>();
//    private final Map<String, MapComponentEntity> components = new HashMap<>();
//    private final Map<String, MapAuthenticationFlowEntity> authenticationFlows = new HashMap<>();
//    private final Map<String, MapAuthenticationExecutionEntity> authenticationExecutions = new HashMap<>();
//    private final Map<String, MapRequiredCredentialEntity> requiredCredentials = new HashMap<>();
//    private final Map<String, MapAuthenticatorConfigEntity> authenticatorConfigs = new HashMap<>();
//    private final Map<String, MapIdentityProviderEntity> identityProviders = new HashMap<>();
//    private final Map<String, MapIdentityProviderMapperEntity> identityProviderMappers = new HashMap<>();
//    private final Map<String, MapFederationEntity> identityProviderFederations = new HashMap<>();
//    private final Map<String, MapRequiredActionProviderEntity> requiredActionProviders = new HashMap<>();


        @Override
        public String getId() {
            return this.id;
        }

        @Override
        public void setId(String id) {
            if (this.id != null) throw new IllegalStateException("Id cannot be changed");
            this.id = id;
            this.updated |= id != null;
        }

        @Override
        public boolean isUpdated() {
            return this.updated
                    || Optional.ofNullable(getAuthenticationExecutions()).orElseGet(Collections::emptySet).stream().anyMatch(MapAuthenticationExecutionEntity::isUpdated)
                    || Optional.ofNullable(getAuthenticationFlows()).orElseGet(Collections::emptySet).stream().anyMatch(MapAuthenticationFlowEntity::isUpdated)
                    || Optional.ofNullable(getAuthenticatorConfigs()).orElseGet(Collections::emptySet).stream().anyMatch(MapAuthenticatorConfigEntity::isUpdated)
                    || Optional.ofNullable(getClientInitialAccesses()).orElseGet(Collections::emptySet).stream().anyMatch(MapClientInitialAccessEntity::isUpdated)
                    || Optional.ofNullable(getComponents()).orElseGet(Collections::emptySet).stream().anyMatch(MapComponentEntity::isUpdated)
                    || Optional.ofNullable(getIdentityProviders()).orElseGet(Collections::emptySet).stream().anyMatch(MapIdentityProviderEntity::isUpdated)
                    || Optional.ofNullable(getIdentityProviderMappers()).orElseGet(Collections::emptySet).stream().anyMatch(MapIdentityProviderMapperEntity::isUpdated)
                    || Optional.ofNullable(getRequiredActionProviders()).orElseGet(Collections::emptySet).stream().anyMatch(MapRequiredActionProviderEntity::isUpdated)
                    || Optional.ofNullable(getRequiredCredentials()).orElseGet(Collections::emptySet).stream().anyMatch(MapRequiredCredentialEntity::isUpdated)
                    || Optional.ofNullable(getOTPPolicy()).map(MapOTPPolicyEntity::isUpdated).orElse(false)
                    || Optional.ofNullable(getWebAuthnPolicy()).map(MapWebAuthnPolicyEntity::isUpdated).orElse(false)
                    || Optional.ofNullable(getWebAuthnPolicyPasswordless()).map(MapWebAuthnPolicyEntity::isUpdated).orElse(false);
        }

        @Override
        public void clearUpdatedFlag() {
            this.updated = false;
            Optional.ofNullable(getAuthenticationExecutions()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getAuthenticationFlows()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getAuthenticatorConfigs()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getClientInitialAccesses()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getComponents()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getIdentityProviders()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getIdentityProviderMappers()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getRequiredActionProviders()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getRequiredCredentials()).orElseGet(Collections::emptySet).forEach(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getOTPPolicy()).ifPresent(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getWebAuthnPolicy()).ifPresent(UpdatableEntity::clearUpdatedFlag);
            Optional.ofNullable(getWebAuthnPolicyPasswordless()).ifPresent(UpdatableEntity::clearUpdatedFlag);
        }

        @Override
        public Optional<MapComponentEntity> getComponent(String componentId) {
            Set<MapComponentEntity> cs = getComponents();
            if (cs == null || cs.isEmpty()) return Optional.empty();

            return cs.stream().filter(c -> Objects.equals(c.getId(), componentId)).findFirst();
        }

        @Override
        public Boolean removeComponent(String componentId) {
            Set<MapComponentEntity> cs = getComponents();
            boolean removed = cs != null && cs.removeIf(c -> Objects.equals(c.getId(), componentId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapAuthenticationFlowEntity> getAuthenticationFlow(String flowId) {
            Set<MapAuthenticationFlowEntity> afs = getAuthenticationFlows();
            if (afs == null || afs.isEmpty()) return Optional.empty();

            return afs.stream().filter(afe -> Objects.equals(afe.getId(), flowId)).findFirst();
        }

        @Override
        public Boolean removeAuthenticationFlow(String flowId) {
            Set<MapAuthenticationFlowEntity> afs = getAuthenticationFlows();
            boolean removed = afs != null && afs.removeIf(af -> Objects.equals(af.getId(), flowId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapAuthenticationExecutionEntity> getAuthenticationExecution(String executionId) {
            Set<MapAuthenticationExecutionEntity> aes = getAuthenticationExecutions();
            if (aes == null || aes.isEmpty()) return Optional.empty();

            return aes.stream().filter(ae -> Objects.equals(ae.getId(), executionId)).findFirst();
        }

        @Override
        public Boolean removeAuthenticationExecution(String executionId) {
            Set<MapAuthenticationExecutionEntity> aes = getAuthenticationExecutions();
            boolean removed = aes != null && aes.removeIf(ae -> Objects.equals(ae.getId(), executionId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapAuthenticatorConfigEntity> getAuthenticatorConfig(String authenticatorConfigId) {
            Set<MapAuthenticatorConfigEntity> acs = getAuthenticatorConfigs();
            if (acs == null || acs.isEmpty()) return Optional.empty();

            return acs.stream().filter(ac -> Objects.equals(ac.getId(), authenticatorConfigId)).findFirst();
        }

        @Override
        public Boolean removeAuthenticatorConfig(String authenticatorConfigId) {
            Set<MapAuthenticatorConfigEntity> acs = getAuthenticatorConfigs();
            boolean removed = acs != null && acs.removeIf(ac -> Objects.equals(ac.getId(), authenticatorConfigId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapRequiredActionProviderEntity> getRequiredActionProvider(String requiredActionProviderId) {
            Set<MapRequiredActionProviderEntity> raps = getRequiredActionProviders();
            if (raps == null || raps.isEmpty()) return Optional.empty();

            return raps.stream().filter(ac -> Objects.equals(ac.getId(), requiredActionProviderId)).findFirst();
        }

        @Override
        public Boolean removeRequiredActionProvider(String requiredActionProviderId) {
            Set<MapRequiredActionProviderEntity> raps = getRequiredActionProviders();
            boolean removed = raps != null && raps.removeIf(rap -> Objects.equals(rap.getId(), requiredActionProviderId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Boolean removeIdentityProvider(String identityProviderId) {
            Set<MapIdentityProviderEntity> ips = getIdentityProviders();
            boolean removed = ips != null && ips.removeIf(ip -> Objects.equals(ip.getId(), identityProviderId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapIdentityProviderMapperEntity> getIdentityProviderMapper(String identityProviderMapperId) {
            Set<MapIdentityProviderMapperEntity> ipms = getIdentityProviderMappers();
            if (ipms == null || ipms.isEmpty()) return Optional.empty();

            return ipms.stream().filter(ipm -> Objects.equals(ipm.getId(), identityProviderMapperId)).findFirst();
        }

        @Override
        public Boolean removeIdentityProviderMapper(String identityProviderMapperId) {
            Set<MapIdentityProviderMapperEntity> ipms = getIdentityProviderMappers();
            boolean removed = ipms != null && ipms.removeIf(ipm -> Objects.equals(ipm.getId(), identityProviderMapperId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public Optional<MapClientInitialAccessEntity> getClientInitialAccess(String clientInitialAccessId) {
            Set<MapClientInitialAccessEntity> cias = getClientInitialAccesses();
            if (cias == null || cias.isEmpty()) return Optional.empty();

            return cias.stream().filter(cia -> Objects.equals(cia.getId(), clientInitialAccessId)).findFirst();
        }

        @Override
        public Boolean removeClientInitialAccess(String clientInitialAccessId) {
            Set<MapClientInitialAccessEntity> cias = getClientInitialAccesses();
            boolean removed = cias != null && cias.removeIf(cia -> Objects.equals(cia.getId(), clientInitialAccessId));
            this.updated |= removed;
            return removed;
        }

        @Override
        public void removeExpiredClientInitialAccesses() {
            Set<MapClientInitialAccessEntity> cias = getClientInitialAccesses();
            if (cias != null)
                cias.stream()
                    .filter(this::checkIfExpired)
                    .map(MapClientInitialAccessEntity::getId)
                    .collect(Collectors.toSet())
                    .forEach(this::removeClientInitialAccess);
        }

        @Override
        public boolean hasClientInitialAccess() {
            Set<MapClientInitialAccessEntity> cias = getClientInitialAccesses();
            return cias != null && !cias.isEmpty();
        }

        private boolean checkIfExpired(MapClientInitialAccessEntity cia) {
            return cia.getRemainingCount() < 1 ||
                    (cia.getExpiration() > 0 && (cia.getTimestamp() + cia.getExpiration()) < Time.currentTime());
        }
    }

    String getName();
    void setName(String name);

    String getDisplayName();
    void setDisplayName(String displayName);

    String getDisplayNameHtml();
    void setDisplayNameHtml(String displayNameHtml);

    Boolean isEnabled();
    void setEnabled(Boolean enabled);

    Boolean isRegistrationAllowed();
    void setRegistrationAllowed(Boolean registrationAllowed);

    Boolean isRegistrationEmailAsUsername();
    void setRegistrationEmailAsUsername(Boolean registrationEmailAsUsername);

    Boolean isVerifyEmail();
    void setVerifyEmail(Boolean verifyEmail);

    Boolean isResetPasswordAllowed();
    void setResetPasswordAllowed(Boolean resetPasswordAllowed);

    Boolean isLoginWithEmailAllowed();
    void setLoginWithEmailAllowed(Boolean loginWithEmailAllowed);

    Boolean isDuplicateEmailsAllowed();
    void setDuplicateEmailsAllowed(Boolean duplicateEmailsAllowed);

    Boolean isRememberMe();
    void setRememberMe(Boolean rememberMe);

    Boolean isEditUsernameAllowed();
    void setEditUsernameAllowed(Boolean editUsernameAllowed);

    Boolean isRevokeRefreshToken();
    void setRevokeRefreshToken(Boolean revokeRefreshToken);

    Boolean isAdminEventsEnabled();
    void setAdminEventsEnabled(Boolean adminEventsEnabled);

    Boolean isAdminEventsDetailsEnabled();
    void setAdminEventsDetailsEnabled(Boolean adminEventsDetailsEnabled);

    Boolean isInternationalizationEnabled();
    void setInternationalizationEnabled(Boolean internationalizationEnabled);

    Boolean isAllowUserManagedAccess();
    void setAllowUserManagedAccess(Boolean allowUserManagedAccess);

    Boolean isOfflineSessionMaxLifespanEnabled();
    void setOfflineSessionMaxLifespanEnabled(Boolean offlineSessionMaxLifespanEnabled);

    Boolean isEventsEnabled();
    void setEventsEnabled(Boolean eventsEnabled);

    Integer getRefreshTokenMaxReuse();
    void setRefreshTokenMaxReuse(Integer refreshTokenMaxReuse);

    Integer getSsoSessionIdleTimeout();
    void setSsoSessionIdleTimeout(Integer ssoSessionIdleTimeout);

    Integer getSsoSessionMaxLifespan();
    void setSsoSessionMaxLifespan(Integer ssoSessionMaxLifespan);

    Integer getSsoSessionIdleTimeoutRememberMe();
    void setSsoSessionIdleTimeoutRememberMe(Integer ssoSessionIdleTimeoutRememberMe);

    Integer getSsoSessionMaxLifespanRememberMe();
    void setSsoSessionMaxLifespanRememberMe(Integer ssoSessionMaxLifespanRememberMe);

    Integer getOfflineSessionIdleTimeout();
    void setOfflineSessionIdleTimeout(Integer offlineSessionIdleTimeout);

    Integer getAccessTokenLifespan();
    void setAccessTokenLifespan(Integer accessTokenLifespan);

    Integer getAccessTokenLifespanForImplicitFlow();
    void setAccessTokenLifespanForImplicitFlow(Integer accessTokenLifespanForImplicitFlow);

    Integer getAccessCodeLifespan();
    void setAccessCodeLifespan(Integer accessCodeLifespan);

    Integer getAccessCodeLifespanUserAction();
    void setAccessCodeLifespanUserAction(Integer accessCodeLifespanUserAction);

    Integer getAccessCodeLifespanLogin();
    void setAccessCodeLifespanLogin(Integer accessCodeLifespanLogin);

    Long getNotBefore();
    void setNotBefore(Long notBefore);

    Integer getClientSessionIdleTimeout();
    void setClientSessionIdleTimeout(Integer clientSessionIdleTimeout);

    Integer getClientSessionMaxLifespan();
    void setClientSessionMaxLifespan(Integer clientSessionMaxLifespan);

    Integer getClientOfflineSessionIdleTimeout();
    void setClientOfflineSessionIdleTimeout(Integer clientOfflineSessionIdleTimeout);

    Integer getClientOfflineSessionMaxLifespan();
    void setClientOfflineSessionMaxLifespan(Integer clientOfflineSessionMaxLifespan);

    Integer getActionTokenGeneratedByAdminLifespan();
    void setActionTokenGeneratedByAdminLifespan(Integer actionTokenGeneratedByAdminLifespan);

    Integer getOfflineSessionMaxLifespan();
    void setOfflineSessionMaxLifespan(Integer offlineSessionMaxLifespan);

    Long getEventsExpiration();
    void setEventsExpiration(Long eventsExpiration);

    String getPasswordPolicy();
    void setPasswordPolicy(String passwordPolicy);

    String getSslRequired();
    void setSslRequired(String sslRequired);

    String getLoginTheme();
    void setLoginTheme(String loginTheme);

    String getAccountTheme();
    void setAccountTheme(String accountTheme);

    String getAdminTheme();
    void setAdminTheme(String adminTheme);

    String getEmailTheme();
    void setEmailTheme(String emailTheme);

    String getMasterAdminClient();
    void setMasterAdminClient(String masterAdminClient);

    String getDefaultRoleId();
    void setDefaultRoleId(String defaultRoleId);

    String getDefaultLocale();
    void setDefaultLocale(String defaultLocale);

    String getBrowserFlow();
    void setBrowserFlow(String browserFlow);

    String getRegistrationFlow();
    void setRegistrationFlow(String registrationFlow);

    String getDirectGrantFlow();
    void setDirectGrantFlow(String directGrantFlow);

    String getResetCredentialsFlow();
    void setResetCredentialsFlow(String resetCredentialsFlow);

    String getClientAuthenticationFlow();
    void setClientAuthenticationFlow(String clientAuthenticationFlow);

    String getDockerAuthenticationFlow();
    void setDockerAuthenticationFlow(String dockerAuthenticationFlow);

    MapOTPPolicyEntity getOTPPolicy();
    void setOTPPolicy(MapOTPPolicyEntity otpPolicy);

    MapWebAuthnPolicyEntity getWebAuthnPolicy();
    void setWebAuthnPolicy(MapWebAuthnPolicyEntity webAuthnPolicy);

    MapWebAuthnPolicyEntity getWebAuthnPolicyPasswordless();
    void setWebAuthnPolicyPasswordless(MapWebAuthnPolicyEntity webAuthnPolicyPasswordless);

    Set<String> getDefaultClientScopeIds();
    void addDefaultClientScopeId(String scopeId);
    Boolean removeDefaultClientScopeId(String scopeId);

    Set<String> getOptionalClientScopeIds();
    void addOptionalClientScopeId(String scopeId);
    Boolean removeOptionalClientScopeId(String scopeId);

    Set<String> getDefaultGroupIds();
    void addDefaultGroupId(String groupId);
    void removeDefaultGroupId(String groupId);

    Set<String> getEventsListeners();
    void setEventsListeners(Set<String> eventsListeners);

    Set<String> getEnabledEventTypes();
    void setEnabledEventTypes(Set<String> enabledEventTypes);

    Set<String> getSupportedLocales();
    void setSupportedLocales(Set<String> supportedLocales);

    Map<String, Map<String, String>> getLocalizationTexts();
    Map<String, String> getLocalizationText(String locale);
    void setLocalizationText(String locale, Map<String, String> texts);
    Boolean removeLocalizationText(String locale);

    Map<String, String> getBrowserSecurityHeaders();
    void setBrowserSecurityHeaders(Map<String, String> headers);
    void setBrowserSecurityHeader(String name, String value);

    Map<String, String> getSmtpConfig();
    void setSmtpConfig(Map<String, String> smtpConfig);

    Set<MapRequiredCredentialEntity> getRequiredCredentials();
    void addRequiredCredential(MapRequiredCredentialEntity requiredCredential);

    Set<MapComponentEntity> getComponents();
    Optional<MapComponentEntity> getComponent(String id);
    void addComponent(MapComponentEntity component);
    Boolean removeComponent(String componentId);

    Set<MapAuthenticationFlowEntity> getAuthenticationFlows();
    Optional<MapAuthenticationFlowEntity> getAuthenticationFlow(String flowId);
    void addAuthenticationFlow(MapAuthenticationFlowEntity authenticationFlow);
    Boolean removeAuthenticationFlow(String flowId);

    Set<MapAuthenticationExecutionEntity> getAuthenticationExecutions();
    Optional<MapAuthenticationExecutionEntity> getAuthenticationExecution(String id);
    void addAuthenticationExecution(MapAuthenticationExecutionEntity authenticationExecution);
    Boolean removeAuthenticationExecution(String executionId);

    Set<MapAuthenticatorConfigEntity> getAuthenticatorConfigs();
    void addAuthenticatorConfig(MapAuthenticatorConfigEntity authenticatorConfig);
    Optional<MapAuthenticatorConfigEntity> getAuthenticatorConfig(String authenticatorConfigId);
    Boolean removeAuthenticatorConfig(String authenticatorConfigId);

    Set<MapRequiredActionProviderEntity> getRequiredActionProviders();
    void addRequiredActionProvider(MapRequiredActionProviderEntity requiredActionProvider);
    Optional<MapRequiredActionProviderEntity> getRequiredActionProvider(String requiredActionProviderId);
    Boolean removeRequiredActionProvider(String requiredActionProviderId);

    Set<MapIdentityProviderEntity> getIdentityProviders();
    void addIdentityProvider(MapIdentityProviderEntity identityProvider);
    Boolean removeIdentityProvider(String identityProviderId);

    Set<MapIdentityProviderMapperEntity> getIdentityProviderMappers();
    void addIdentityProviderMapper(MapIdentityProviderMapperEntity identityProviderMapper);
    Boolean removeIdentityProviderMapper(String identityProviderMapperId);
    Optional<MapIdentityProviderMapperEntity> getIdentityProviderMapper(String identityProviderMapperId);

    Set<MapClientInitialAccessEntity> getClientInitialAccesses();
    void addClientInitialAccess(MapClientInitialAccessEntity clientInitialAccess);
    Optional<MapClientInitialAccessEntity> getClientInitialAccess(String clientInitialAccessId);
    Boolean removeClientInitialAccess(String clientInitialAccessId);
    @IgnoreForEntityImplementationGenerator
    void removeExpiredClientInitialAccesses();
    @IgnoreForEntityImplementationGenerator
    boolean hasClientInitialAccess();
//
//    public MapFederationEntity getIdentityProvidersFederationById(String id){
//        return identityProviderFederations.get(id);
//    }
//
//    public Stream<MapFederationEntity> getIdentityProvidersFederations(){
//        return identityProviderFederations.values().stream();
//    }
//
//    public void addIdentityProvidersFederation(MapFederationEntity idpFederation) {
//        if (identityProviderFederations.containsKey(idpFederation.getInternalId())) {
//            throw new ModelDuplicateException("An IdentityProviderFederation with given id already exists");
//        }
//        this.updated = true;
//        identityProviderFederations.put(idpFederation.getInternalId(), idpFederation);
//    }
//
//    public boolean removeIdentityProvidersFederation(String id) {
//        boolean removed = this.identityProviderFederations.remove(id) != null;
//        updated |= removed;
//        return removed;
//    }
//
//    public void updateIdentityProvidersFederation(MapFederationEntity idpFederation) {
//        this.updated |= identityProviderFederations.replace(idpFederation.getInternalId(), idpFederation) != null;
//    }
//
//    public void addIdentityProvidersFederationMapper(FederationMapperModel federationMapperModel) {
//        this.updated = true;
//        this.identityProviderFederations.get(federationMapperModel.getFederationId()).addFederationMapper(MapFederationMapperEntity.fromModel(federationMapperModel));
//    };
//
//    public  void updateIdentityProvidersFederationMapper(FederationMapperModel federationMapperModel) {
//        this.updated = true;
//        MapFederationEntity federationEntity = this.identityProviderFederations.get(federationMapperModel.getFederationId());
//        MapFederationMapperEntity mapperEntity = MapFederationMapperEntity.fromModel(federationMapperModel);
//        federationEntity.getFederationMappers().remove(mapperEntity);
//        federationEntity.addFederationMapper(mapperEntity);
//    };
//
//    public void removeIdentityProvidersFederationMapper(String id,String federationId) {
//        this.updated = true;
//        MapFederationEntity federationEntity = this.identityProviderFederations.get(federationId);
//        federationEntity.removeFederationMapper(id);
//    };
//
//
//    public void removeFederationIdp(FederationModel idpFederation, String idpAlias){
//        if (!identityProviderFederations.containsKey(idpFederation.getInternalId()))
//            throw new ModelException("An IdentityProviderFederation with given id does not exist. Cannot remove identity provider of that federation");
//        if (idpAlias==null || idpAlias.isEmpty())
//            throw new ModelException("Impossible to remove an identity provider, provided a null or empty alias");
//        MapIdentityProviderEntity currentIdp = getIdentityProviders().filter(i -> i.getAlias().equals(idpAlias)).findFirst().orElse(null);
//        if(currentIdp==null)
//            throw new ModelException("There's no identity provider by that alias in this federation.");
//        if(currentIdp.getFederations()!=null && currentIdp.getFederations().size()==1){ //remove the idp, since it belongs only in this federation
//            this.removeIdentityProvider(currentIdp.getId());
//        }
//        else if(currentIdp.getFederations()!=null ) {
//            currentIdp.getFederations().remove(idpFederation.getInternalId());
//            this.updateIdentityProvider(currentIdp);
//        }
//    }
}
