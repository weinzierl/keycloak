package org.keycloak.models.cache.infinispan.events;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.Set;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.cache.infinispan.IdpCacheManager;

@SerializeWith(IdentityProviderUpdatedEvent.ExternalizerImpl.class)
public class IdentityProviderUpdatedEvent extends InvalidationEvent implements IdentityProviderCacheInvalidationEvent {

	private String id;
	private String alias;
    private String realmId;

    public static IdentityProviderUpdatedEvent create(String id, String alias, String realmId) {
    	IdentityProviderUpdatedEvent event = new IdentityProviderUpdatedEvent();
    	event.id = id;
    	event.alias = alias;
        event.realmId = realmId;
        return event;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format("IdentityProviderUpdatedEvent [ realmId=%s, id=%s, alias=%s ]", realmId, id, alias);
    }

    @Override
    public void addInvalidations(IdpCacheManager idpCache, Set<String> invalidations) {
        idpCache.identityProviderUpdated(id, alias, realmId, invalidations);
    }

    public static class ExternalizerImpl implements Externalizer<IdentityProviderUpdatedEvent> {

        private static final int VERSION_1 = 1;

        @Override
        public void writeObject(ObjectOutput output, IdentityProviderUpdatedEvent obj) throws IOException {
            output.writeByte(VERSION_1);

            MarshallUtil.marshallString(obj.id, output);
            MarshallUtil.marshallString(obj.alias, output);
            MarshallUtil.marshallString(obj.realmId, output);
        }

        @Override
        public IdentityProviderUpdatedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            switch (input.readByte()) {
                case VERSION_1:
                    return readObjectVersion1(input);
                default:
                    throw new IOException("Unknown version");
            }
        }

        public IdentityProviderUpdatedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
        	IdentityProviderUpdatedEvent res = new IdentityProviderUpdatedEvent();
        	res.id = MarshallUtil.unmarshallString(input);
        	res.alias = MarshallUtil.unmarshallString(input);
            res.realmId = MarshallUtil.unmarshallString(input);

            return res;
        }
    }
}
