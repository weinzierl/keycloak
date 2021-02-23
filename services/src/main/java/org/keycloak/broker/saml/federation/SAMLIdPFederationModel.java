package org.keycloak.broker.saml.federation;

import static org.keycloak.broker.saml.SAMLConfigNames.*;

import org.keycloak.models.IdentityProvidersFederationModel;

public class SAMLIdPFederationModel extends IdentityProvidersFederationModel {
	
	public SAMLIdPFederationModel(){
		super();
	}
	
	public SAMLIdPFederationModel(IdentityProvidersFederationModel model) {
		super(model);
	}

    public String getNameIDPolicyFormat() {
        return getConfig().get(NAME_ID_POLICY_FORMAT);
    }
    
    public String getPrincipalType() {
        return getConfig().get(PRINCIPAL_TYPE);
    }
    
    public String getPrincipalAttribute() {
        return getConfig().get(PRINCIPAL_ATTRIBUTE);
    }

    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_AUTHN_REQUESTS_SIGNED));
    }

    public boolean isWantAssertionsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_SIGNED));
    }

    public boolean isWantAssertionsEncrypted() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_ENCRYPTED));
    }
    
    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_AUTHN_REQUEST));
    }
	
}
