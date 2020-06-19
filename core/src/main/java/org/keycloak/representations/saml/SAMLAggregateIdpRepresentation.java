package org.keycloak.representations.saml;

public class SAMLAggregateIdpRepresentation {
  
  private String entityId;
  private String diplayName;
  
  public String getEntityId() {
    return entityId;
  }
  public void setEntityId(String entityId) {
    this.entityId = entityId;
  }
  public String getDiplayName() {
    return diplayName;
  }
  public void setDiplayName(String diplayName) {
    this.diplayName = diplayName;
  }

}
