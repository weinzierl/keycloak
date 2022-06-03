package org.keycloak.partialimport;

import java.util.List;
import java.util.stream.Collectors;

import org.keycloak.broker.federation.IdpFederationProvider;
import org.keycloak.broker.federation.IdpFederationProviderFactory;
import org.keycloak.models.FederationMapperModel;
import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.models.utils.RepresentationToModel;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;
import org.keycloak.representations.idm.PartialImportRepresentation;

public class IdentityProvidersFederationPartialImport extends AbstractPartialImport<IdentityProvidersFederationRepresentation> {

    @Override
    public List<IdentityProvidersFederationRepresentation> getRepList(PartialImportRepresentation partialImportRep) {
        return partialImportRep.getIdentityProvidersFederations();
    }

    @Override
    public String getName(IdentityProvidersFederationRepresentation rep) {
        return rep.getAlias();
    }

    @Override
    public String getModelId(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation rep) {
        return rep.getInternalId();
    }

    @Override
    public boolean exists(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation rep) {
        return realm.getIdentityProvidersFederationById(rep.getInternalId()) != null;
    }

    @Override
    public String existsMessage(RealmModel realm, IdentityProvidersFederationRepresentation metadataAggregate) {
        return "SAML Federation'" + getName(metadataAggregate) + "' already exists.";
    }

    @Override
    public ResourceType getResourceType() {
        return ResourceType.SAML_FEDERATION;
    }

    @Override
    public void remove(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation rep) {
        IdentityProvidersFederationModel model = RepresentationToModel.toModel(rep);
        IdpFederationProvider federationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session,model,realm.getId());
        federationProvider.removeFederation();
    }

    @Override
    public void create(RealmModel realm, KeycloakSession session, IdentityProvidersFederationRepresentation rep) {
        rep.setInternalId(KeycloakModelUtils.generateId());
        IdentityProvidersFederationModel model = RepresentationToModel.toModel(rep);
        realm.addIdentityProvidersFederation(model);
        rep.getFederationMappers().stream().forEach(mapper -> {
            mapper.setFederationId(rep.getInternalId());
            FederationMapperModel mapperModel = RepresentationToModel.toModel(mapper);
            realm.addIdentityProvidersFederationMapper(mapperModel);
        });
        IdpFederationProvider federationProvider = IdpFederationProviderFactory.getIdpFederationProviderFactoryById(session, model.getProviderId()).create(session,model,realm.getId());
        federationProvider.enableUpdateTask();
    }

}
