package org.keycloak.protocol.oidc.federation.tasks;

import org.keycloak.models.KeycloakSession;

public class ClientExpiryTasks implements ClientExpiryTasksI {

    private KeycloakSession session;
    
    
    public ClientExpiryTasks(KeycloakSession session) {
        this.session = session;
        //TODO: add here the business logic for setting the clients -> expired
        setExpiryTask();
    }
    
    
    
    private void setExpiryTask() {
        System.out.println("Setting task to check expired clients");
        session.realms().getRealms().stream().forEach(realm -> realm.getClients().stream().forEach(client -> {
            System.out.println(String.format("Client name=%s   and id=%s ",client.getName(), client.getId()));
        }));
    }
    
    
    @Override
    public void close() {
        
    }
    
}
