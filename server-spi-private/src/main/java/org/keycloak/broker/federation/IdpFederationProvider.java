package org.keycloak.broker.federation;

import java.io.InputStream;
import java.util.Set;

import org.keycloak.models.IdentityProvidersFederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.Provider;

public interface IdpFederationProvider <C extends IdentityProvidersFederationModel> extends Provider {

	Set<String> parseIdps(KeycloakSession session, InputStream inputStream);

	String updateFederation();

	void updateIdentityProviders();

	void removeFederation();

	void enableUpdateTask();

	
	
}
