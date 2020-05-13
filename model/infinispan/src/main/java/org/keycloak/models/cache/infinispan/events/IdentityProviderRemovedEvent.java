//package org.keycloak.models.cache.infinispan.events;
//
//import java.io.IOException;
//import java.io.ObjectInput;
//import java.io.ObjectOutput;
//import java.util.Set;
//
//import org.infinispan.commons.marshall.Externalizer;
//import org.infinispan.commons.marshall.MarshallUtil;
//import org.keycloak.models.cache.infinispan.IdpCacheManager;
//
//public class IdentityProviderRemovedEvent extends InvalidationEvent implements IdentityProviderCacheInvalidationEvent {
//
//	private String id;
//	private String alias;
//    private String realmId;
//	
//
//    public static IdentityProviderRemovedEvent create(String id, String alias, String realmId) {
//    	IdentityProviderRemovedEvent event = new IdentityProviderRemovedEvent();
//    	event.id = id;
//    	event.alias = alias;
//        event.realmId = realmId;
//        return event;
//    }
//
//    @Override
//    public String getId() {
//        return id;
//    }
//
//    @Override
//    public String toString() {
//        return String.format("IdentityProviderRemovedEvent [ realmId=%s, id=%s, alias=%s ]", realmId, id, alias);
//    }
//
//    @Override
//    public void addInvalidations(IdpCacheManager idpCache, Set<String> invalidations) {
//        idpCache.identityProviderRemoved(id, invalidations);
//    }
//
//    public static class ExternalizerImpl implements Externalizer<IdentityProviderRemovedEvent> {
//
//        private static final int VERSION_1 = 1;
//
//        @Override
//        public void writeObject(ObjectOutput output, IdentityProviderRemovedEvent obj) throws IOException {
//            output.writeByte(VERSION_1);
//
//            MarshallUtil.marshallString(obj.id, output);
//            MarshallUtil.marshallString(obj.alias, output);
//            MarshallUtil.marshallString(obj.realmId, output);
//        }
//
//        @Override
//        public IdentityProviderRemovedEvent readObject(ObjectInput input) throws IOException, ClassNotFoundException {
//            switch (input.readByte()) {
//                case VERSION_1:
//                    return readObjectVersion1(input);
//                default:
//                    throw new IOException("Unknown version");
//            }
//        }
//
//        public IdentityProviderRemovedEvent readObjectVersion1(ObjectInput input) throws IOException, ClassNotFoundException {
//        	IdentityProviderRemovedEvent res = new IdentityProviderRemovedEvent();
//        	res.id = MarshallUtil.unmarshallString(input);
//        	res.alias = MarshallUtil.unmarshallString(input);
//            res.realmId = MarshallUtil.unmarshallString(input);
//
//            return res;
//        }
//    }
//	
//}
