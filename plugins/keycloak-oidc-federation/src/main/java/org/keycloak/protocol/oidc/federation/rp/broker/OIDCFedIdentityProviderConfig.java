package org.keycloak.protocol.oidc.federation.rp.broker;

import static org.keycloak.common.util.UriUtils.checkUrl;

import java.io.IOException;
import java.util.List;

import org.keycloak.broker.oidc.OIDCIdentityProviderConfig;
import org.keycloak.common.enums.SslRequired;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.RealmModel;
import org.keycloak.util.JsonSerialization;

public class OIDCFedIdentityProviderConfig extends OIDCIdentityProviderConfig {
    
    public static final String CLIENT_REGISTRATION_TYPES = "clientRegistrationTypes";    
    public static final String ORGANIZATION_NAME = "organizationName";
    public static final String AUTHORITY_HINTS = "authorityHints";
    public static final String EXPIRED = "expired";
    public static final String TRUST_ANCHOR_ID = "trustAnchorId";

    public OIDCFedIdentityProviderConfig() {
        super();
    }
    
    public OIDCFedIdentityProviderConfig(IdentityProviderModel model) {
        super(model);
    }
    
    public String getClientRegistrationTypes() {
        return getConfig().get(CLIENT_REGISTRATION_TYPES);
    }
    public void setClientRegistrationTypes(String clientRegistrationTypes) {
        getConfig().put(CLIENT_REGISTRATION_TYPES, clientRegistrationTypes);
    }
    
    public String getOrganizationName() {
        return getConfig().get(ORGANIZATION_NAME);
    }
    public void setOrganizationName(String organizationName) {
        getConfig().put(ORGANIZATION_NAME, organizationName);
    }
    
    public List<String> getAuthorityHints() throws IOException {
        return JsonSerialization.readValue(getConfig().get(AUTHORITY_HINTS), List.class) ;
    }
    public void setAuthorityHints(List<String> authorityHints) {
        try {
            getConfig().put(AUTHORITY_HINTS, JsonSerialization.writeValueAsString(authorityHints) );
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    
    public Long getExpired() {
        return Long.valueOf(getConfig().get(EXPIRED));
    }
    public void setExpired(Long expired) {
        getConfig().put(EXPIRED, expired.toString());
    }
    
    public String getTrustAnchorId() {
        return getConfig().get(TRUST_ANCHOR_ID);
    }
    public void setTrustAnchorId(String trustAnchorId) {
        getConfig().put(TRUST_ANCHOR_ID, trustAnchorId);
    }
    
    @Override
    public void validate(RealmModel realm) {
        super.validate(realm);
        try {
            getAuthorityHints().stream().forEach(auth->  checkUrl(SslRequired.NONE, auth, "Authority hints"));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}