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
import java.net.URI;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import javax.xml.namespace.QName;
import javax.xml.parsers.ParserConfigurationException;

import org.jboss.logging.Logger;
import org.keycloak.broker.federation.AbstractIdPFederationProvider;
import org.keycloak.broker.saml.SAMLIdentityProviderConfig;
import org.keycloak.common.util.PemUtils;
import org.keycloak.connections.httpclient.HttpClientProvider;
import org.keycloak.crypto.Algorithm;
import org.keycloak.crypto.KeyStatus;
import org.keycloak.crypto.KeyUse;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.metadata.EndpointType;
import org.keycloak.dom.saml.v2.metadata.EntitiesDescriptorType;
import org.keycloak.dom.saml.v2.metadata.EntityAttributes;
import org.keycloak.dom.saml.v2.metadata.EntityDescriptorType;
import org.keycloak.dom.saml.v2.metadata.IDPSSODescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyDescriptorType;
import org.keycloak.dom.saml.v2.metadata.KeyTypes;
import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.keys.RsaKeyMetadata;
import org.keycloak.models.AuthenticationFlowModel;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.ModelException;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.DefaultAuthenticationFlows;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.protocol.saml.SamlService;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.saml.SPMetadataDescriptor;
import org.keycloak.saml.common.constants.GeneralConstants;
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
	
	
	
	public SAMLIdPFederationProvider(KeycloakSession session, SAMLIdPFederationModel model,String realmId) {
		super(session, model,realmId);
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
		RealmModel realm = session.realms().getRealm(realmId);
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
		UpdateFederationIdentityProviders updateFederationIdentityProviders = new UpdateFederationIdentityProviders(model.getInternalId(),realmId);
		ScheduledTaskRunner taskRunner = new ScheduledTaskRunner(session.getKeycloakSessionFactory(),updateFederationIdentityProviders);
		long delay = (model.getUpdateFrequencyInMins() * 60 * 1000) - Instant.now().toEpochMilli() + model.getLastMetadataRefreshTimestamp();
		timer.schedule(taskRunner, delay > 60 * 1000 ? delay : 60 * 1000, model.getUpdateFrequencyInMins() * 60 * 1000, "UpdateFederation" + model.getInternalId());
	}
	
	@Override
	public void updateIdentityProviders() {
		
		logger.info("Started updating IdPs of federation (id): " + model.getInternalId());
		
		RealmModel realm = session.realms().getRealm(realmId);
		
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
		
		//default language
		//should be changed to default realm???
		final String preferredLang = "en";
		
		//default authedication flow model
		AuthenticationFlowModel flowModel = realm.getFlowByAlias(DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW);
        if (flowModel == null) {
            throw new ModelException("No available authentication flow with alias: " + DefaultAuthenticationFlows.FIRST_BROKER_LOGIN_FLOW);
        }

		for(EntityDescriptorType entity: entities) {
			
			//conditional add (if it's in the skiplist)
			if(model.getSkipIdps().contains(entity.getEntityID()))
				continue;
						
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
				logger.infof("The entity %s is not an Identity provider!", entity.getEntityID());
				continue;
			}
			

            String alias = getHash(entity.getEntityID());
            IdentityProviderModel identityProviderModel = null;
            
            //check if this federation has already included this IdP
            if (model.getIdentityprovidersAlias().contains(alias)) {
                identityProviderModel = new SAMLIdentityProviderConfig(
                    session.identityProviderStorage().getIdentityProviderByAlias(realm, alias));
                model.getIdentityprovidersAlias().remove(alias);
            } else {

                // check if Idp exists in database
                IdentityProviderModel previous = session.identityProviderStorage().getIdentityProviderByAlias(realm, alias);
                if (previous != null) {
                    identityProviderModel = new SAMLIdentityProviderConfig(
                        session.identityProviderStorage().getIdentityProviderByAlias(realm, alias));
                } else {

                    // initialize idp values
                    // set alias and default values
                    identityProviderModel = new SAMLIdentityProviderConfig();
                    identityProviderModel.setProviderId(model.getProviderId());
                    identityProviderModel.setAlias(alias);
                    // put default parameters
                    Map<String, String> config = new HashMap<>();
                    config.put("addExtensionsElementWithKeyInfo", "false");
                    config.put("signatureAlgorithm", "RSA_SHA256");
                    config.put("samlXmlKeyNameTranformer", "KEY_ID");
                    config.put("principalType", "SUBJECT");
                    config.put(IdentityProviderModel.SYNC_MODE, "IMPORT");
                    config.put("loginHint", "false");
                    identityProviderModel.setConfig(config);

                    identityProviderModel.setFirstBrokerLoginFlowId(flowModel.getId());
                }
                identityProviderModel.addFederation(model.getInternalId());
            }
            parseIdP(identityProviderModel, validUntil, entity, idpDescriptor, preferredLang);
            
			try {
			    identityProviderModel.validate(realm);
				session.identityProviderStorage().addFederationIdp(realm, identityProviderModel);
			}
			catch(Exception ex) {
				ex.printStackTrace();
				logger.infof("Federation: %s -> Could not insert the identity provider with entityId: %s", model.getDisplayName(), entity.getEntityID());
			}
				
			
		}
		
		model.getIdentityprovidersAlias().stream().forEach(idpAlias ->  session.identityProviderStorage().removeFederationIdp(realm, model, idpAlias));
		
		model.setLastMetadataRefreshTimestamp(new Date().getTime());
		realm.updateIdentityProvidersFederation(model);

		logger.info("Finished updating IdPs of federation (id): " + model.getInternalId());
	}
	
    private void parseIdP(IdentityProviderModel identityProviderModel, Date validUntil, EntityDescriptorType entity,
        IDPSSODescriptorType idpDescriptor, String preferredLang) {
        if (validUntil.before(new Date())) {
            identityProviderModel.setEnabled(false);
        } else {
            identityProviderModel.setEnabled(true);
        }

        LocalizedNameType displayName = idpDescriptor.getExtensions() != null
            && idpDescriptor.getExtensions().getUIInfo() != null
                ? idpDescriptor.getExtensions().getUIInfo().getDisplayName().stream()
                    .filter(dn -> preferredLang.equals(dn.getLang())).findAny().orElse(null)
                : null;
        if (displayName != null) {
            identityProviderModel.setDisplayName(displayName.getValue());
        } else {
            displayName = entity.getOrganization().getOrganizationDisplayName().stream()
                .filter(dn -> preferredLang.equals(dn.getLang())).findAny()
                .orElse(entity.getOrganization().getOrganizationDisplayName().stream()
                    .filter(dn -> preferredLang.equals(dn.getLang())).findAny().orElse(null));
            if (displayName != null)
                identityProviderModel.setDisplayName(displayName.getValue());
            else
                identityProviderModel.setDisplayName(entity.getEntityID());

        }

        parseIDPSSODescriptorType(identityProviderModel, idpDescriptor);

        // check for hide on login attibute - for update if condition is false set value to false
        identityProviderModel.getConfig().put("hideOnLoginPage", "false");
        if (entity.getExtensions() != null && entity.getExtensions().getEntityAttributes() != null) {
            for (AttributeType attribute : entity.getExtensions().getEntityAttributes().getAttribute()) {
                if (GeneralConstants.MACEDIR.equals(attribute.getName())
                    && attribute.getAttributeValue().contains(GeneralConstants.HIDE_FOR_DISCOVERY))
                    identityProviderModel.getConfig().put("hideOnLoginPage", "true");
            }

        }
    }
	
	private void parseIDPSSODescriptorType (IdentityProviderModel identityProviderModel, IDPSSODescriptorType idpDescriptor) {
	
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
		
		identityProviderModel.getConfig().put("singleLogoutServiceUrl", singleLogoutServiceUrl);
		identityProviderModel.getConfig().put("singleSignOnServiceUrl", singleSignOnServiceUrl);
		identityProviderModel.getConfig().put("wantAuthnRequestsSigned", idpDescriptor.isWantAuthnRequestsSigned().toString());
		identityProviderModel.getConfig().put("validateSignature", idpDescriptor.isWantAuthnRequestsSigned().toString());
		identityProviderModel.getConfig().put("postBindingResponse", postBindingResponse.toString());
		identityProviderModel.getConfig().put("postBindingAuthnRequest", postBindingResponse.toString());
		identityProviderModel.getConfig().put("postBindingLogout", postBindingLogout.toString());
		

		List<KeyDescriptorType> keyDescriptor = idpDescriptor.getKeyDescriptor();
		String defaultCertificate = null;

		if (keyDescriptor != null) {
			for (KeyDescriptorType keyDescriptorType : keyDescriptor) {
				Element keyInfo = keyDescriptorType.getKeyInfo();
				Element x509KeyInfo = DocumentUtil.getChildElement(keyInfo,
						new QName("dsig", "X509Certificate"));

				if (KeyTypes.SIGNING.equals(keyDescriptorType.getUse())) {
				    identityProviderModel.getConfig().put("signingCertificate", x509KeyInfo.getTextContent());
				} else if (KeyTypes.ENCRYPTION.equals(keyDescriptorType.getUse())) {
				    identityProviderModel.getConfig().put("encryptionPublicKey", x509KeyInfo.getTextContent());
				} else if (keyDescriptorType.getUse() == null) {
					defaultCertificate = x509KeyInfo.getTextContent();
				}
			}
		}

		if (defaultCertificate != null) {
			
			//array certificate
			if (identityProviderModel.getConfig().get("signingCertificate")== null) {
			    identityProviderModel.getConfig().put("signingCertificate", defaultCertificate);
			}

			if (identityProviderModel.getConfig().get("encryptionPublicKey")== null) {
			    identityProviderModel.getConfig().put("encryptionPublicKey", defaultCertificate);
			}
		}
		
		//saml aggregate metadata config parameters
		if ( model.getConfig().get("wantAssertionsEncrypted") != null )
		    identityProviderModel.getConfig().put("wantAssertionsEncrypted", model.getConfig().get("wantAssertionsEncrypted"));
		
		if ( model.getConfig().get("wantAssertionsSigned") != null )
		    identityProviderModel.getConfig().put("wantAssertionsSigned", model.getConfig().get("wantAssertionsSigned"));
		
        List<String> nameIdFormatList = idpDescriptor.getNameIDFormat();
        if (nameIdFormatList != null && !nameIdFormatList.isEmpty()) {
            identityProviderModel.getConfig().put("nameIDPolicyFormat", nameIdFormatList.get(0));
        } else if ( identityProviderModel.getConfig().get("nameIDPolicyFormat") == null) {
            //for creation only set default nameIDPolicyFormat
            identityProviderModel.getConfig().put("nameIDPolicyFormat",
                model.getConfig().get("nameIDPolicyFormat") != null ? model.getConfig().get("nameIDPolicyFormat")
                    : JBossSAMLURIConstants.NAMEID_FORMAT_PERSISTENT.get());
        }
	}
	
	
	@Override
	public void removeFederation() {
		
		logger.debug("Removing federation " + model.getInternalId() + " and all its IdPs");
		
		//cancel federation update task
		TimerProvider timer = session.getProvider(TimerProvider.class);
		timer.cancelTask("UpdateFederation" + model.getInternalId());
		
		RealmModel realm = session.realms().getRealm(realmId);
		
		List<Boolean> results = model.getIdentityprovidersAlias().stream().map(idpAlias -> session.identityProviderStorage().removeFederationIdp(realm, model, idpAlias)).collect(Collectors.toList());
		
		realm.removeIdentityProvidersFederation(model.getInternalId());
	}
	
	
	@Override
	public void close() {
		// TODO Auto-generated method stub
	}

    
	
	
	public static String getHash(String str) {
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

	
	
	@Override
    public Response export(UriInfo uriInfo, RealmModel realm) {

        try {
            URI authnBinding = JBossSAMLURIConstants.SAML_HTTP_REDIRECT_BINDING.getUri();

            if (model.isPostBindingAuthnRequest())
                authnBinding = JBossSAMLURIConstants.SAML_HTTP_POST_BINDING.getUri();

            URI endpoint = uriInfo.getBaseUriBuilder().path("realms").path(realm.getName()).path("broker").path("endpoint")
                .build();

            boolean wantAuthnRequestsSigned = model.isWantAuthnRequestsSigned();
            boolean wantAssertionsSigned = model.isWantAssertionsSigned();
            boolean wantAssertionsEncrypted = model.isWantAssertionsEncrypted();
            String entityId = getEntityId(uriInfo, realm);
            String nameIDPolicyFormat = model.getNameIDPolicyFormat();

            List<Element> signingKeys = new LinkedList<>();
            List<Element> encryptionKeys = new LinkedList<>();

            session.keys().getKeysStream(realm, KeyUse.SIG, Algorithm.RS256).filter(Objects::nonNull)
                .filter(key -> key.getCertificate() != null).sorted(SamlService::compareKeys).forEach(key -> {
                    try {
                        Element element = SPMetadataDescriptor.buildKeyInfoElement(key.getKid(),
                            PemUtils.encodeCertificate(key.getCertificate()));
                        signingKeys.add(element);

                        if (key.getStatus() == KeyStatus.ACTIVE) {
                            encryptionKeys.add(element);
                        }
                    } catch (ParserConfigurationException e) {
                        logger.warn("Failed to export SAML SP Metadata!", e);
                        throw new RuntimeException(e);
                    }
                });

            String descriptor = SPMetadataDescriptor.getSPDescriptor(authnBinding, endpoint, endpoint, wantAuthnRequestsSigned,
                wantAssertionsSigned, wantAssertionsEncrypted, entityId, nameIDPolicyFormat, signingKeys, encryptionKeys);

            return Response.ok(descriptor, MediaType.APPLICATION_XML_TYPE).build();
        } catch (Exception e) {
            logger.warn("Failed to export SAML SP Metadata!", e);
            throw new RuntimeException(e);
        }
    }
	
	private String getEntityId(UriInfo uriInfo, RealmModel realm) {
        return UriBuilder.fromUri(uriInfo.getBaseUri()).path("realms").path(realm.getName()).build().toString();
    }
	
}
