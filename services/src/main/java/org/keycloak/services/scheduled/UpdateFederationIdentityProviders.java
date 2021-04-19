package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.broker.federation.IdpFederationProviderFactory;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;


import org.keycloak.timer.ScheduledTask;



public class UpdateFederationIdentityProviders implements ScheduledTask {
	
	protected static final Logger logger = Logger.getLogger(UpdateFederationIdentityProviders.class);
	
	protected final String federationId;
	protected final String realmId;
	
	public UpdateFederationIdentityProviders(String federationId,String realmId) {
		this.federationId = federationId;
		this.realmId = realmId;
	}

	@Override
	public void run(KeycloakSession session) {
		logger.info(" Updating identity providers of federation with id " + federationId + " and alias " + realmId);
		IdentityProvidersFederationModel federationModel = session.realms().getRealm(realmId).getIdentityProvidersFederationById(federationId);
		IdpFederationProviderFactory idpFederationProviderFactory = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, federationModel.getProviderId());
		IdpFederationProvider idpFederationProvider = idpFederationProviderFactory.create(session,federationModel,realmId);
		idpFederationProvider.updateIdentityProviders();
	}
	


}
