package org.keycloak.models.cache.infinispan.events;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@SerializeWith(IdpAddedEvent.ExternalizerImpl.class)
public class IdpAddedEvent implements ClusterEvent {
    public static String EVENT_NAME = "IDP_ADDED_EVENT";

    private String realmId;
    private IdentityProviderModel identityProvider;

    public IdpAddedEvent() { }

    public IdpAddedEvent(String realmId, IdentityProviderModel identityProvider) {
        this.realmId = realmId;
        this.identityProvider = identityProvider;
    }

    public String getRealmId() {
        return realmId;
    }

    public void setRealmId(String realmId) {
        this.realmId = realmId;
    }

    public IdentityProviderModel getIdentityProvider() {
        return identityProvider;
    }

    public void setIdentityProvider(IdentityProviderModel identityProvider) {
        this.identityProvider = identityProvider;
    }

    public static class ExternalizerImpl implements Externalizer<IdpAddedEvent> {

        @Override
        public void writeObject(ObjectOutput output, IdpAddedEvent obj) throws IOException {
            MarshallUtil.marshallString(obj.getRealmId(), output);
            MarshallUtil.marshallByteArray(JsonSerialization.writeValueAsBytes(obj.getIdentityProvider()), output);
        }

        @Override
        public IdpAddedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            String realmId = MarshallUtil.unmarshallString(input);
            IdentityProviderModel idpModel = JsonSerialization.readValue(MarshallUtil.unmarshallByteArray(input), IdentityProviderModel.class);
            return new IdpAddedEvent(realmId, idpModel);
        }

    }

}
