package org.keycloak.models.cache.infinispan.entities;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.IdentityProviderMapperModel;

@SerializeWith(CachedIdentityProviderMappers.ExternalizerImpl.class)
public class CachedIdentityProviderMappers {

    String realmId;
    Map<String, IdentityProviderMapperModel> identityProviderMappers;
    
    public CachedIdentityProviderMappers(String realmId, Map<String, IdentityProviderMapperModel> identityProviderMappers) {
        this.realmId = realmId;
        this.identityProviderMappers = identityProviderMappers;
    }

    public Map<String, IdentityProviderMapperModel> getIdentityProviderMappers() {
        return identityProviderMappers;
    }

    public String getRealmId() {
        return realmId;
    }

    public static class ExternalizerImpl implements Externalizer<CachedIdentityProviderMappers> {

        @Override
        public void writeObject(ObjectOutput output, CachedIdentityProviderMappers obj) throws IOException {
            MarshallUtil.marshallString(obj.realmId, output);
            MarshallUtil.marshallMap(obj.identityProviderMappers, output);
        }

        @Override
        public CachedIdentityProviderMappers readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            String realmId = MarshallUtil.unmarshallString(input);
            Map<String, IdentityProviderMapperModel> identityProviderMappers = MarshallUtil.unmarshallMap(input, new MarshallUtil.MapBuilder<String, IdentityProviderMapperModel, Map<String, IdentityProviderMapperModel>>() {
                @Override
                public Map<String, IdentityProviderMapperModel> build(int size) {
                    return new HashMap<>(size);
                }
            });
            return new CachedIdentityProviderMappers(realmId, identityProviderMappers);
        }
    }
    
    
}
