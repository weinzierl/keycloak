package org.keycloak.services.scheduled;

import org.jboss.logging.Logger;
import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.broker.federation.IdpFederationProviderFactory;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.UIInfoType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.xml.datatype.XMLGregorianCalendar;



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
		IdpFederationProvider idpFederationProvider = idpFederationProviderFactory.create(session,federationModel);
		idpFederationProvider.updateIdentityProviders();
	}
	


}
