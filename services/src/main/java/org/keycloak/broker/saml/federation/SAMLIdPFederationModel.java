package org.keycloak.broker.saml.federation;

import static org.keycloak.broker.saml.SAMLConfigNames.*;

import com.fasterxml.jackson.core.type.TypeReference;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.LinkedList;

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

    public LinkedList<SAMLIdentityProviderConfig.Principal> getMultiplePrincipals() throws IOException {
        String principalsJson =getConfig().get(MULTIPLE_PRINCIPALS);
        if (principalsJson != null ) {
            return JsonSerialization.readValue(principalsJson, new TypeReference<LinkedList<SAMLIdentityProviderConfig.Principal>>() {
            });
        } else {
            return new LinkedList<>();
        }
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