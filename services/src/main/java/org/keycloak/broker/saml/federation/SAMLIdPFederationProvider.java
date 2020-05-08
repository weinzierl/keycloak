/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.keycloak.broker.saml.federation;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import javax.xml.namespace.QName;

import org.jboss.logging.Logger;
import org.keycloak.broker.federation.AbstractIdPFederationProvider;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.DocumentUtil;
import org.keycloak.saml.processing.core.parsers.saml.SAMLParser;
import org.keycloak.services.scheduled.ScheduledTaskRunner;
import org.keycloak.services.scheduled.UpdateFederationIdentityProviders;
import org.keycloak.timer.TimerProvider;
import org.w3c.dom.Element;


public class SAMLIdPFederationProvider extends AbstractIdPFederationProvider <SAMLIdPFederationModel> {
    
	protected static final Logger logger = Logger.getLogger(SAMLIdPFederationProvider.class);
	
	
	
	public SAMLIdPFederationProvider(KeycloakSession session, SAMLIdPFederationModel model) {
		super(session, model);
	}

	@Override
    public Set<String> parseIdps(KeycloakSession session, InputStream inputStream) {
        try {
            Object parsedObject = SAMLParser.getInstance().parse(inputStream);
            EntitiesDescriptorType entitiesDescriptorType = (EntitiesDescriptorType) parsedObject;
            List<EntityDescriptorType> entities = (List<EntityDescriptorType>) (Object) entitiesDescriptorType.getEntityDescriptor();
            Set<String> idpIDs = new HashSet<String>();
            for(EntityDescriptorType entity : entities) 
            	idpIDs.add(entity.getEntityID());
            return idpIDs;
        } catch (ParsingException pe) {
            throw new RuntimeException("Could not parse IdP SAML Metadata", pe);
        }
    }
	
	@Override
	public String updateFederation() {
		RealmModel realm = session.realms().getRealm(model.getRealmId());
		IdentityProvidersFederationModel oldModel = realm.getIdentityProvidersFederationById(model.getInternalId());
		if (oldModel == null) {
			model.setInternalId(KeycloakModelUtils.generateId());
			realm.addIdentityProvidersFederation(model);
		} else {
			realm.updateIdentityProvidersFederation(model);
			if (!model.getUpdateFrequencyInMins().equals(oldModel.getUpdateFrequencyInMins()))
				enableUpdateTask();
		}
		return model.getInternalId();
	}
	
	@Override
	public void enableUpdateTask() {
		if(model.getLastMetadataRefreshTimestamp()==null) model.setLastMetadataRefreshTimestamp(0L);
		// remove previous task and add new with new RefreshEveryHours
		TimerProvider timer = session.getProvider(TimerProvider.class);
		timer.cancelTask("UpdateFederation" + model.getInternalId());
		UpdateFederationIdentityProviders updateFederationIdentityProviders = new UpdateFederationIdentityProviders(model.getInternalId(),model.getRealmId());
		ScheduledTaskRunner taskRunner = new ScheduledTaskRunner(session.getKeycloakSessionFactory(),updateFederationIdentityProviders);
		long delay = (model.getUpdateFrequencyInMins() * 60 * 1000) - Instant.now().toEpochMilli() + model.getLastMetadataRefreshTimestamp();
		timer.schedule(taskRunner, delay > 60 * 1000 ? delay : 60 * 1000, model.getUpdateFrequencyInMins() * 60 * 1000, "UpdateFederation" + model.getInternalId());
	}
	
	@Override
	public void updateIdentityProviders() {
		
		logger.info("Started updating IdPs of federation (id): " + model.getInternalId());
		
		RealmModel realm = session.realms().getRealm(model.getRealmId());
		
		List<EntityDescriptorType> entities = new ArrayList<EntityDescriptorType>();
		Date validUntil = null;
		try {
			InputStream inputStream = session.getProvider(HttpClientProvider.class).get(model.getUrl());
			Object parsedObject = SAMLParser.getInstance().parse(inputStream);
			EntitiesDescriptorType entitiesDescriptorType = (EntitiesDescriptorType) parsedObject;
			validUntil = entitiesDescriptorType.getValidUntil().toGregorianCalendar().getTime();
			model.setValidUntilTimestamp(validUntil.getTime());
	        entities = (List<EntityDescriptorType>) (Object) entitiesDescriptorType.getEntityDescriptor();
		} catch (ParsingException | IOException e) {
			e.printStackTrace();
		}
        
		if(validUntil == null || entities.isEmpty())
			return; //add a log entry for the failure reason and/or write it in the database, so you can alert later on the admins through the UI
		
		final String preferredLang = "en";

		for(EntityDescriptorType entity: entities) {
			
			//conditional add (if it's in the skiplist)
			if(model.getSkipIdps().contains(entity.getEntityID()))
				continue;
			
			String alias = getHash(entity.getEntityID());
			
			if(model.getIdentityprovidersAlias().remove(alias)) {
				//TODO: idp already exists, so you might want to perform an update on the existing idp
				continue;
			}
			
			IDPSSODescriptorType idpDescriptor = null;

			// Metadata documents can contain multiple Descriptors (See ADFS metadata
			// documents) such as RoleDescriptor, SPSSODescriptor, IDPSSODescriptor.
			// So we need to loop through to find the IDPSSODescriptor.
			for (EntityDescriptorType.EDTChoiceType edtChoiceType : entity.getChoiceType()) {
				List<EntityDescriptorType.EDTDescriptorChoiceType> descriptors = edtChoiceType.getDescriptors();

				if (!descriptors.isEmpty() && descriptors.get(0).getIdpDescriptor() != null) {
					idpDescriptor = descriptors.get(0).getIdpDescriptor();
				}
			}
			
			if (idpDescriptor == null) {
				//temporary code - we parse only IdPs
				logger.info("These entity is not an Identity provider!");
				continue;
			}
					
			IdentityProviderRepresentation representation = new IdentityProviderRepresentation();
			representation.setProviderId(model.getProviderId());
			representation.setAlias(alias);
			
			if(validUntil.before(new Date()))
				representation.setEnabled(false);
		
			
			LocalizedNameType displayName = idpDescriptor.getExtensions() != null
					&& idpDescriptor.getExtensions().getUIInfo() != null
							? idpDescriptor.getExtensions().getUIInfo().getDisplayName().stream()
									.filter(dn -> preferredLang.equals(dn.getLang())).findAny().orElse(null)
							: null;
			if (displayName != null) {
				representation.setDisplayName(displayName.getValue());
			} else {
				displayName = entity.getOrganization().getOrganizationDisplayName().stream()
						.filter(dn -> preferredLang.equals(dn.getLang())).findAny()
						.orElse(entity.getOrganization().getOrganizationDisplayName().stream()
								.filter(dn -> preferredLang.equals(dn.getLang())).findAny().orElse(null));
				if (displayName != null)
					representation.setDisplayName(displayName.getValue());
				else
					representation.setDisplayName(entity.getEntityID()); 
			
			}
			
			representation.setConfig(parseIDPSSODescriptorType(idpDescriptor));
			
			IdentityProviderModel identityProviderModel = RepresentationToModel.toModel(realm, representation);
	        boolean successful = false;
			try {
				successful = realm.addFederationIdp(model, identityProviderModel);
			}
			catch(Exception ex) {
				successful = false;
			}
			if(!successful)
				logger.infof("Federation: %s -> Could not insert the identity provider with entityId: %s", model.getDisplayName(), entity.getEntityID());
			
		}
		
		model.getIdentityprovidersAlias().stream().forEach(idpAlias ->  realm.removeFederationIdp(model, idpAlias));
		
        //update also the federation entity with totals and failed entities
		updateFederation();

		logger.info("Finished updating IdPs of federation (id): " + model.getInternalId());
	}
	
	private Map<String,String> parseIDPSSODescriptorType (IDPSSODescriptorType idpDescriptor) {
	
		//same as saml idp parsing
		Map<String,String> config = new HashMap<>();
		
		String singleSignOnServiceUrl = null;
		Boolean postBindingResponse = Boolean.FALSE;
		Boolean postBindingLogout = Boolean.FALSE;
		for (EndpointType endpoint : idpDescriptor.getSingleSignOnService()) {
			if (endpoint.getBinding().toString().equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
				singleSignOnServiceUrl = endpoint.getLocation().toString();
				postBindingResponse = Boolean.TRUE;
				break;
			} else if (endpoint.getBinding().toString()
					.equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
				singleSignOnServiceUrl = endpoint.getLocation().toString();
			}
		}
		String singleLogoutServiceUrl = null;
		for (EndpointType endpoint : idpDescriptor.getSingleLogoutService()) {
			if (postBindingResponse && endpoint.getBinding().toString()
					.equals(JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.get())) {
				singleLogoutServiceUrl = endpoint.getLocation().toString();
				postBindingLogout = Boolean.TRUE;
				break;
			} else if (!postBindingResponse && endpoint.getBinding().toString()
					.equals(JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.get())) {
				singleLogoutServiceUrl = endpoint.getLocation().toString();
				break;
			}

		}
		
		config.put("singleLogoutServiceUrl", singleLogoutServiceUrl);
		config.put("singleSignOnServiceUrl", singleSignOnServiceUrl);
		config.put("wantAuthnRequestsSigned", idpDescriptor.isWantAuthnRequestsSigned().toString());
		config.put("addExtensionsElementWithKeyInfo", "false");
		config.put("validateSignature", idpDescriptor.isWantAuthnRequestsSigned().toString());
		config.put("postBindingResponse", postBindingResponse.toString());
		config.put("postBindingAuthnRequest", postBindingResponse.toString());
		config.put("postBindingLogout", postBindingLogout.toString());

		List<KeyDescriptorType> keyDescriptor = idpDescriptor.getKeyDescriptor();
		String defaultCertificate = null;

		if (keyDescriptor != null) {
			for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
				Element keyInfo = keyDescriptorType.getKeyInfo();
				Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo,
						new QName("dsig", "X509Certificate"));

				if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
					config.put("signingCertificate", x509KeyInfo.getTextContent());
				} else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
					config.put("encryptionPublicKey", x509KeyInfo.getTextContent());
				} else if (keyDescriptorType.getUse() == null) {
					defaultCertificate = x509KeyInfo.getTextContent();
				}
			}
		}

		if (defaultCertificate != null) {
			
			//array certificate
			if (config.get("signingCertificate")== null) {
				config.put("signingCertificate", defaultCertificate);
			}

			if (config.get("encryptionPublicKey")== null) {
				config.put("encryptionPublicKey", defaultCertificate);
			}
		}
		
		//put default parameters
		config.put("nameIDPolicyFormat", JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());
		config.put("signatureAlgorithm","RSA_SHA256");
		config.put("samlXmlKeyNameTranformer", "KEY_ID");
		config.put("principalType", "SUBJECT");
		
		return config;
	}
	
	
	@Override
	public void removeFederation() {
		
		logger.debug("Removing federation " + model.getInternalId() + " and all its IdPs");
		
		//cancel federation update task
		TimerProvider timer = session.getProvider(TimerProvider.class);
		timer.cancelTask("UpdateFederation" + model.getInternalId());
		
		RealmModel realm = session.realms().getRealm(model.getRealmId());
		List<Boolean> results = model.getIdentityprovidersAlias().stream().map(idpAlias -> realm.removeFederationIdp(model, idpAlias)).collect(Collectors.toList());
		
		realm.removeIdentityProvidersFederation(model.getInternalId());
	}
	
	
	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

    
	
	
	private static String getHash(String str) {
		byte[] hashBytes;
		try {
			hashBytes = MessageDigest.getInstance("SHA-256").digest(str.getBytes());
		}
		catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
		StringBuilder sb = new StringBuilder();
        for(byte b : hashBytes)
            sb.append(String.format("%02x", b));
        return sb.toString();
	}
	
	
	
}
