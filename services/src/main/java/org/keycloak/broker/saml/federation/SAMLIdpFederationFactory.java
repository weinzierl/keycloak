package org.keycloak.broker.saml.federation;

import org.keycloak.Config.Scope;
import org.keycloak.broker.federation.AbstractIdPFederationProviderFactory;
import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.broker.saml.federation.SAMLIdPFederationProvider;


public class SAMLIdpFederationFactory extends AbstractIdPFederationProviderFactory<SAMLIdPFederationProvider> {

	public static final String FEDERATION_PROVIDER_ID = "saml";  //same as the SAMLIdentityProviderFactory.PROVIDER_ID
	
	
	@Override
	public String getId() {
		return FEDERATION_PROVIDER_ID;
	}
	
	@Override
    public String getName() {
        return "SAML v2.0 federation";
    }


	
	@Override
	public SAMLIdPFederationProvider create(KeycloakSession session, IdentityProvidersFederationModel model) {
		return new SAMLIdPFederationProvider(session, new SAMLIdPFederationModel(model));
	}
	

	
	@Override
	public void init(Scope config) {
		// TODO Auto-generated method stub
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
		// TODO Auto-generated method stub
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
	}





	

}
