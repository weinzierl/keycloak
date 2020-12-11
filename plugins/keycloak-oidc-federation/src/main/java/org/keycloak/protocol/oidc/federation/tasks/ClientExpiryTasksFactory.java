package org.keycloak.protocol.oidc.federation.tasks;

import org.keycloak.Config.Scope;
import org.keycloak.models.ClientModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.models.KeycloakSessionTask;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserConsentModel;
import org.keycloak.models.UserModel;
import org.keycloak.models.dblock.DBLockManager;
import org.keycloak.models.dblock.DBLockProvider;
import org.keycloak.models.utils.KeycloakModelUtils;
import org.keycloak.provider.ProviderFactory;

public class ClientExpiryTasksFactory implements ClientExpiryTasksFactoryI {

    public static final String PROVIDER_ID = "client-expiry-tasks";
    
    @Override
    public ClientExpiryTasks create(KeycloakSession session) {
        System.out.println("Instantiating the ClientExpiryTasks class");
        return new ClientExpiryTasks(session);
    }

    @Override
    public void init(Scope config) {
        System.out.println("\n\nINIT\n\n");
    }

    @Override
    public void postInit(KeycloakSessionFactory sessionFactory) {
        System.out.println("\n\nPOST-INIT\n\n");
    }

    @Override
    public void close() {
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }
    
    
}
