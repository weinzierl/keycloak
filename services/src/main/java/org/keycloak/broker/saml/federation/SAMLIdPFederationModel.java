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

    public boolean isWantAuthnRequestsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_AUTHN_REQUESTS_SIGNED));
    }

    public boolean isWantAssertionsSigned() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_SIGNED));
    }

    public boolean isWantAssertionsEncrypted() {
        return Boolean.valueOf(getConfig().get(WANT_ASSERTIONS_ENCRYPTED));
    }

    public boolean isAddExtensionsElementWithKeyInfo() {
        return Boolean.valueOf(getConfig().get(ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO));
    }

    public boolean isPostBindingAuthnRequest() {
        return Boolean.valueOf(getConfig().get(POST_BINDING_AUTHN_REQUEST));
    }
	
}
