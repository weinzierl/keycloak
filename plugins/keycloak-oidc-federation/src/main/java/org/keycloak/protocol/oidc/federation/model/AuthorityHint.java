package org.keycloak.protocol.oidc.federation.model;

import javax.persistence.Access;
import javax.persistence.AccessType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;

@Entity
@Table(name="AUTHORITY_HINT"
, uniqueConstraints = {
      @UniqueConstraint(columnNames = { "REALM_ID", "VALUE" })
}
)
@NamedQueries({ @NamedQuery(name = "findAuthorityHintByRealm", query = "from AuthorityHint where realmId = :realmId") })
public class AuthorityHint {

    @Id
    @Column(name="ID", length = 36)
    @Access(AccessType.PROPERTY) 
    private String id;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @Column(name = "VALUE", nullable = false)
    private String value;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
    
    

}
