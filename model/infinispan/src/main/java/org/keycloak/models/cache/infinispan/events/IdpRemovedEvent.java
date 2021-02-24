package org.keycloak.models.cache.infinispan.events;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.cluster.ClusterEvent;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.models.cache.infinispan.events.serialization.EventSerializer;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

@SerializeWith(IdpRemovedEvent.ExternalizerImpl.class)
public class IdpRemovedEvent implements ClusterEvent {
    public static String EVENT_NAME = "IDP_REMOVED_EVENT";

    private String realmId;
    private IdentityProviderModel identityProvider;

    public IdpRemovedEvent(){ }

    public IdpRemovedEvent(String realmId, IdentityProviderModel identityProvider) {
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


    public static class ExternalizerImpl implements Externalizer<IdpRemovedEvent> {

        @Override
        public void writeObject(ObjectOutput output, IdpRemovedEvent obj) throws IOException {
            MarshallUtil.marshallString(obj.getRealmId(), output);
            MarshallUtil.marshallByteArray(EventSerializer.writeValueAsBytes(obj.getIdentityProvider()), output);
        }

        @Override
        public IdpRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            String realmId = MarshallUtil.unmarshallString(input);
            IdentityProviderModel idpModel = EventSerializer.readValue(MarshallUtil.unmarshallByteArray(input), IdentityProviderModel.class);
            return new IdpRemovedEvent(realmId, idpModel);
        }

    }
}
