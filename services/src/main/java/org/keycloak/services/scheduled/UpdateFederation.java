package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.broker.federation.FederationProvider;
import org.keycloak.broker.federation.SAMLFederationProviderFactory;
import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;


import org.keycloak.timer.ScheduledTask;



public class UpdateFederation implements ScheduledTask {
	
	protected static final Logger logger = Logger.getLogger(UpdateFederation.class);
	
	protected final String federationId;
	protected final String realmId;
	
	public UpdateFederation(String federationId, String realmId) {
		this.federationId = federationId;
		this.realmId = realmId;
	}

	@Override
	public void run(KeycloakSession session) {
		logger.info(" Updating identity providers of federation with id " + federationId + " and alias " + realmId);
		FederationModel federationModel = session.realms().getRealm(realmId).getSAMLFederationById(federationId);
		SAMLFederationProviderFactory samlFederationProviderFactory = SAMLFederationProviderFactory.getSAMLFederationProviderFactoryById(session, federationModel.getProviderId());
		FederationProvider federationProvider = samlFederationProviderFactory.create(session,federationModel,realmId);
		federationProvider.updateIdentityProviders();
	}
	


}
