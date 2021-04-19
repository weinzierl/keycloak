package org.keycloak.broker.federation;

import java.io.InputStream;
import java.util.Set;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.keycloak.models.FederationModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.provider.Provider;

public interface FederationProvider<C extends FederationModel> extends Provider {

	Set<String> parseIdps(KeycloakSession session, InputStream inputStream);

	String updateFederation();

	void updateIdentityProviders();

	void removeFederation();

	void enableUpdateTask();

	Response export(UriInfo uriInfo, RealmModel realm);
	
}
