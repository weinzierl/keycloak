package org.keycloak.partialimport;

import java.util.List;

import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.broker.federation.IdpFederationProviderFactory;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

public class IdentityProvidersFederationPartialImport extends AbstractPartialImport<IdentityProvidersFederationRepresentation> {

    @Override
    public List<IdentityProvidersFederationRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getIdentityProvidersFederations();
    }

    @Override
    public String getName(IdentityProvidersFederationRepresentation metadataAggregate) {
        return metadataAggregate.getAlias();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation metadataAggregate) {
        return metadataAggregate.getInternalId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation metadataAggregate) {
        return realm.getIdentityProvidersFederationById(metadataAggregate.getInternalId()) != null;
    }

    @Override
    public String existsMessage(RealmModel realm, IdentityProvidersFederationRepresentation metadataAggregate) {
        return "Metadata Aggregate'" + getName(metadataAggregate) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.METADATA_AGGREGATE;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation metadataAggregate) {
    	IdentityProvidersFederationModel model = RepresentationToModel.toModel(metadataAggregate);
    	model.getIdentityprovidersAlias().stream().forEach(idpAlias -> realm.removeFederationIdp(model, idpAlias));
    	realm.removeIdentityProvidersFederation(model.getInternalId());
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation metadataAggregate) {
    	IdentityProvidersFederationModel model = RepresentationToModel.toModel(metadataAggregate);
        realm.addIdentityProvidersFederation(model);
        IdpFederationProvider idpFederationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session,model,realm.getId());
        idpFederationProvider.enableUpdateTask();
    }

}
