package org.keycloak.partialimport;

import java.util.List;

import org.keycloak.broker.federation.FederationProvider;
import org.keycloak.broker.federation.SAMLFederationProviderFactory;
import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.SAMLFederationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

public class IdentityProvidersFederationPartialImport extends AbstractPartialImport<SAMLFederationRepresentation> {

    @Override
    public List<SAMLFederationRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getIdentityProvidersFederations();
    }

    @Override
    public String getName(SAMLFederationRepresentation metadataAggregate) {
        return metadataAggregate.getAlias();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation metadataAggregate) {
        return metadataAggregate.getInternalId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation metadataAggregate) {
        return realm.getSAMLFederationById(metadataAggregate.getInternalId()) != null;
    }

    @Override
    public String existsMessage(RealmModel realm, SAMLFederationRepresentation metadataAggregate) {
        return "Metadata Aggregate'" + getName(metadataAggregate) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.METADATA_AGGREGATE;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation metadataAggregate) {
    	FederationModel model = RepresentationToModel.toModel(metadataAggregate);
    	List<String> idps = realm.getIdentityProvidersByFederation(model.getInternalId());
        idps.stream().forEach(idpAlias -> realm.removeFederationIdp(model, idpAlias));
    	realm.removeSAMLFederation(model.getInternalId());
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, SAMLFederationRepresentation metadataAggregate) {
    	FederationModel model = RepresentationToModel.toModel(metadataAggregate);
        realm.addSAMLFederation(model);
        FederationProvider federationProvider = SAMLFederationProviderFactory.getSAMLFederationProviderFactoryById(session, model.getProviderId()).create(session,model,realm.getId());
        federationProvider.enableUpdateTask();
    }

}
