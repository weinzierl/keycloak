package org.keycloak.protocol.oidc.federation.beans;

import com.fasterxml.jackson.annotation.JsonProperty;

public class OIDCFederationConfigurationRepresentationPolicy {
    
    @JsonProperty("federation_registration_endpoint")
    private Policy<String> federationRegistrationEndpoint;
    
    @JsonProperty("pushed_authorization_request_endpoint")
    private Policy<String> pushedAuthorizationRequestEndpoint;
    
    @JsonProperty("client_registration_types_supported")
    private Policy<String> clientRegistrationTypesSupported;
    
    //needed?how?
//    @JsonProperty("client_registration_authn_methods_supported")
//    private Map<String,List<String>> clientRegistrationAuthnMethodsSupported;

    @JsonProperty("organization_name")
    private Policy<String> organizationName;
    
    @JsonProperty("issuer")
    private Policy<String> issuer;

    @JsonProperty("authorization_endpoint")
    private Policy<String> authorizationEndpoint;

    @JsonProperty("token_endpoint")
    private Policy<String> tokenEndpoint;

    @JsonProperty("introspection_endpoint")
    private Policy<String> introspectionEndpoint;

    @JsonProperty("userinfo_endpoint")
    private Policy<String> userinfoEndpoint;

    @JsonProperty("end_session_endpoint")
    private Policy<String> logoutEndpoint;

    @JsonProperty("jwks_uri")
    private Policy<String> jwksUri;

    @JsonProperty("check_session_iframe")
    private Policy<String> checkSessionIframe;

    @JsonProperty("grant_types_supported")
    private Policy<String> grantTypesSupported;

    @JsonProperty("response_types_supported")
    private Policy<String> responseTypesSupported;

    @JsonProperty("subject_types_supported")
    private Policy<String> subjectTypesSupported;

    @JsonProperty("id_token_signing_alg_values_supported")
    private Policy<String> idTokenSigningAlgValuesSupported;

    @JsonProperty("id_token_encryption_alg_values_supported")
    private Policy<String> idTokenEncryptionAlgValuesSupported;

    @JsonProperty("id_token_encryption_enc_values_supported")
    private Policy<String> idTokenEncryptionEncValuesSupported;

    @JsonProperty("userinfo_signing_alg_values_supported")
    private Policy<String> userInfoSigningAlgValuesSupported;

    @JsonProperty("request_object_signing_alg_values_supported")
    private Policy<String> requestObjectSigningAlgValuesSupported;

    @JsonProperty("response_modes_supported")
    private Policy<String> responseModesSupported;

    @JsonProperty("registration_endpoint")
    private Policy<String> registrationEndpoint;

    @JsonProperty("token_endpoint_auth_methods_supported")
    private Policy<String> tokenEndpointAuthMethodsSupported;

    @JsonProperty("token_endpoint_auth_signing_alg_values_supported")
    private Policy<String> tokenEndpointAuthSigningAlgValuesSupported;

    @JsonProperty("claims_supported")
    private Policy<String> claimsSupported;

    @JsonProperty("claim_types_supported")
    private Policy<String> claimTypesSupported;

    @JsonProperty("claims_parameter_supported")
    private Policy<Boolean> claimsParameterSupported;

    @JsonProperty("scopes_supported")
    private Policy<String> scopesSupported;

    @JsonProperty("request_parameter_supported")
    private Policy<Boolean> requestParameterSupported;

    @JsonProperty("request_uri_parameter_supported")
    private Policy<Boolean> requestUriParameterSupported;

    @JsonProperty("code_challenge_methods_supported")
    private Policy<String> codeChallengeMethodsSupported;

    @JsonProperty("tls_client_certificate_bound_access_tokens")
    private Policy<Boolean> tlsClientCertificateBoundAccessTokens;

    public Policy<String> getFederationRegistrationEndpoint() {
        return federationRegistrationEndpoint;
    }

    public void setFederationRegistrationEndpoint(Policy<String> federationRegistrationEndpoint) {
        this.federationRegistrationEndpoint = federationRegistrationEndpoint;
    }

    public Policy<String> getPushedAuthorizationRequestEndpoint() {
        return pushedAuthorizationRequestEndpoint;
    }

    public void setPushedAuthorizationRequestEndpoint(Policy<String> pushedAuthorizationRequestEndpoint) {
        this.pushedAuthorizationRequestEndpoint = pushedAuthorizationRequestEndpoint;
    }

    public Policy<String> getClientRegistrationTypesSupported() {
        return clientRegistrationTypesSupported;
    }

    public void setClientRegistrationTypesSupported(Policy<String> clientRegistrationTypesSupported) {
        this.clientRegistrationTypesSupported = clientRegistrationTypesSupported;
    }

    public Policy<String> getOrganizationName() {
        return organizationName;
    }

    public void setOrganizationName(Policy<String> organizationName) {
        this.organizationName = organizationName;
    }

    public Policy<String> getIssuer() {
        return issuer;
    }

    public void setIssuer(Policy<String> issuer) {
        this.issuer = issuer;
    }

    public Policy<String> getAuthorizationEndpoint() {
        return authorizationEndpoint;
    }

    public void setAuthorizationEndpoint(Policy<String> authorizationEndpoint) {
        this.authorizationEndpoint = authorizationEndpoint;
    }

    public Policy<String> getTokenEndpoint() {
        return tokenEndpoint;
    }

    public void setTokenEndpoint(Policy<String> tokenEndpoint) {
        this.tokenEndpoint = tokenEndpoint;
    }

    public Policy<String> getIntrospectionEndpoint() {
        return introspectionEndpoint;
    }

    public void setIntrospectionEndpoint(Policy<String> introspectionEndpoint) {
        this.introspectionEndpoint = introspectionEndpoint;
    }

    public Policy<String> getUserinfoEndpoint() {
        return userinfoEndpoint;
    }

    public void setUserinfoEndpoint(Policy<String> userinfoEndpoint) {
        this.userinfoEndpoint = userinfoEndpoint;
    }

    public Policy<String> getLogoutEndpoint() {
        return logoutEndpoint;
    }

    public void setLogoutEndpoint(Policy<String> logoutEndpoint) {
        this.logoutEndpoint = logoutEndpoint;
    }

    public Policy<String> getJwksUri() {
        return jwksUri;
    }

    public void setJwksUri(Policy<String> jwksUri) {
        this.jwksUri = jwksUri;
    }

    public Policy<String> getCheckSessionIframe() {
        return checkSessionIframe;
    }

    public void setCheckSessionIframe(Policy<String> checkSessionIframe) {
        this.checkSessionIframe = checkSessionIframe;
    }

    public Policy<String> getGrantTypesSupported() {
        return grantTypesSupported;
    }

    public void setGrantTypesSupported(Policy<String> grantTypesSupported) {
        this.grantTypesSupported = grantTypesSupported;
    }

    public Policy<String> getResponseTypesSupported() {
        return responseTypesSupported;
    }

    public void setResponseTypesSupported(Policy<String> responseTypesSupported) {
        this.responseTypesSupported = responseTypesSupported;
    }

    public Policy<String> getSubjectTypesSupported() {
        return subjectTypesSupported;
    }

    public void setSubjectTypesSupported(Policy<String> subjectTypesSupported) {
        this.subjectTypesSupported = subjectTypesSupported;
    }

    public Policy<String> getIdTokenSigningAlgValuesSupported() {
        return idTokenSigningAlgValuesSupported;
    }

    public void setIdTokenSigningAlgValuesSupported(Policy<String> idTokenSigningAlgValuesSupported) {
        this.idTokenSigningAlgValuesSupported = idTokenSigningAlgValuesSupported;
    }

    public Policy<String> getIdTokenEncryptionAlgValuesSupported() {
        return idTokenEncryptionAlgValuesSupported;
    }

    public void setIdTokenEncryptionAlgValuesSupported(Policy<String> idTokenEncryptionAlgValuesSupported) {
        this.idTokenEncryptionAlgValuesSupported = idTokenEncryptionAlgValuesSupported;
    }

    public Policy<String> getIdTokenEncryptionEncValuesSupported() {
        return idTokenEncryptionEncValuesSupported;
    }

    public void setIdTokenEncryptionEncValuesSupported(Policy<String> idTokenEncryptionEncValuesSupported) {
        this.idTokenEncryptionEncValuesSupported = idTokenEncryptionEncValuesSupported;
    }

    public Policy<String> getUserInfoSigningAlgValuesSupported() {
        return userInfoSigningAlgValuesSupported;
    }

    public void setUserInfoSigningAlgValuesSupported(Policy<String> userInfoSigningAlgValuesSupported) {
        this.userInfoSigningAlgValuesSupported = userInfoSigningAlgValuesSupported;
    }

    public Policy<String> getRequestObjectSigningAlgValuesSupported() {
        return requestObjectSigningAlgValuesSupported;
    }

    public void setRequestObjectSigningAlgValuesSupported(Policy<String> requestObjectSigningAlgValuesSupported) {
        this.requestObjectSigningAlgValuesSupported = requestObjectSigningAlgValuesSupported;
    }

    public Policy<String> getResponseModesSupported() {
        return responseModesSupported;
    }

    public void setResponseModesSupported(Policy<String> responseModesSupported) {
        this.responseModesSupported = responseModesSupported;
    }

    public Policy<String> getRegistrationEndpoint() {
        return registrationEndpoint;
    }

    public void setRegistrationEndpoint(Policy<String> registrationEndpoint) {
        this.registrationEndpoint = registrationEndpoint;
    }

    public Policy<String> getTokenEndpointAuthMethodsSupported() {
        return tokenEndpointAuthMethodsSupported;
    }

    public void setTokenEndpointAuthMethodsSupported(Policy<String> tokenEndpointAuthMethodsSupported) {
        this.tokenEndpointAuthMethodsSupported = tokenEndpointAuthMethodsSupported;
    }

    public Policy<String> getTokenEndpointAuthSigningAlgValuesSupported() {
        return tokenEndpointAuthSigningAlgValuesSupported;
    }

    public void setTokenEndpointAuthSigningAlgValuesSupported(Policy<String> tokenEndpointAuthSigningAlgValuesSupported) {
        this.tokenEndpointAuthSigningAlgValuesSupported = tokenEndpointAuthSigningAlgValuesSupported;
    }

    public Policy<String> getClaimsSupported() {
        return claimsSupported;
    }

    public void setClaimsSupported(Policy<String> claimsSupported) {
        this.claimsSupported = claimsSupported;
    }

    public Policy<String> getClaimTypesSupported() {
        return claimTypesSupported;
    }

    public void setClaimTypesSupported(Policy<String> claimTypesSupported) {
        this.claimTypesSupported = claimTypesSupported;
    }

    public Policy<Boolean> getClaimsParameterSupported() {
        return claimsParameterSupported;
    }

    public void setClaimsParameterSupported(Policy<Boolean> claimsParameterSupported) {
        this.claimsParameterSupported = claimsParameterSupported;
    }

    public Policy<String> getScopesSupported() {
        return scopesSupported;
    }

    public void setScopesSupported(Policy<String> scopesSupported) {
        this.scopesSupported = scopesSupported;
    }

    public Policy<Boolean> getRequestParameterSupported() {
        return requestParameterSupported;
    }

    public void setRequestParameterSupported(Policy<Boolean> requestParameterSupported) {
        this.requestParameterSupported = requestParameterSupported;
    }

    public Policy<Boolean> getRequestUriParameterSupported() {
        return requestUriParameterSupported;
    }

    public void setRequestUriParameterSupported(Policy<Boolean> requestUriParameterSupported) {
        this.requestUriParameterSupported = requestUriParameterSupported;
    }

    public Policy<String> getCodeChallengeMethodsSupported() {
        return codeChallengeMethodsSupported;
    }

    public void setCodeChallengeMethodsSupported(Policy<String> codeChallengeMethodsSupported) {
        this.codeChallengeMethodsSupported = codeChallengeMethodsSupported;
    }

    public Policy<Boolean> getTlsClientCertificateBoundAccessTokens() {
        return tlsClientCertificateBoundAccessTokens;
    }

    public void setTlsClientCertificateBoundAccessTokens(Policy<Boolean> tlsClientCertificateBoundAccessTokens) {
        this.tlsClientCertificateBoundAccessTokens = tlsClientCertificateBoundAccessTokens;
    }
    
    

}
