package org.keycloak.broker.saml.federation;

import org.keycloak.models.IdentityProvidersFederationModel;

public class SAMLIdPFederationModel extends IdentityProvidersFederationModel {
	
	public SAMLIdPFederationModel(){
		super();
	}
	
	public SAMLIdPFederationModel(IdentityProvidersFederationModel model) {
		super(model);
	}
	
}
