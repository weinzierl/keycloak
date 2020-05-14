package org.keycloak.models.jpa;

import javax.persistence.EntityManager;

import org.keycloak.Config.Scope;
import org.keycloak.connections.jpa.JpaConnectionProvider;
import org.keycloak.models.IdentityProviderProvider;
import org.keycloak.models.IdpProviderFactory;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;


public class JpaIdpProviderFactory implements IdpProviderFactory {

	@Override
	public IdentityProviderProvider create(KeycloakSession session) {
		EntityManager em = session.getProvider(JpaConnectionProvider.class).getEntityManager();
		return new JpaIdpProvider(session, em);
	}

	@Override
	public void init(Scope config) {
	}

	@Override
	public void postInit(KeycloakSessionFactory factory) {
	}

	@Override
	public void close() {
	}

	@Override
	public String getId() {
		return "jpa";
	}

}