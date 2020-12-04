package org.keycloak.protocol.oidc.federation.model;

import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "OIDC_FEDERATION_CONFIG")
public class ConfigurationEntity {

    @Id
    @Column(name = "REALM_ID", nullable = false)
    private String realmId;
    
    @Column(name = "CONFIGURATION", nullable = false)
    @Convert(converter = ConfigurationJsonConverter.class)
    private Configuration configuration;

    public ConfigurationEntity() {
        
    }
    
    public ConfigurationEntity(String realmId, Configuration configuration) {
        this.realmId = realmId;
        this.configuration = configuration;
    }
    
    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public Configuration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    
}
