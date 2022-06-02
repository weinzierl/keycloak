package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.broker.federation.IdpFederationProviderFactory;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;


import org.keycloak.models.RealmModel;
import org.keycloak.timer.ScheduledTask;
import org.keycloak.timer.TimerProvider;


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
		RealmModel realm = session.realms().getRealm(realmId);
		if ( realm != null) {
			IdentityProvidersFederationModel federationModel = realm.getIdentityProvidersFederationById(federationId);
			IdpFederationProviderFactory samlFederationProviderFactory = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, federationModel.getProviderId());
			IdpFederationProvider federationProvider = samlFederationProviderFactory.create(session, federationModel, realmId);
			federationProvider.updateIdentityProviders();
		} else {
			//realm has been removed. remove this task
			TimerProvider timer = session.getProvider(TimerProvider.class);
			timer.cancelTask("UpdateFederation" + federationId);
		}
	}
	


}
