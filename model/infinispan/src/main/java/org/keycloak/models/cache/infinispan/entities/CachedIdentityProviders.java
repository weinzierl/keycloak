package org.keycloak.models.cache.infinispan.entities;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.Map;

import org.infinispan.commons.marshall.Externalizer;
import org.infinispan.commons.marshall.MarshallUtil;
import org.infinispan.commons.marshall.SerializeWith;
import org.keycloak.models.IdentityProviderModel;

@SerializeWith(CachedIdentityProviders.ExternalizerImpl.class)
public class CachedIdentityProviders {

	Map<String, IdentityProviderModel> identityProviders;
	String realmId;
	
	public CachedIdentityProviders(String realmId, Map<String, IdentityProviderModel> identityProviders) {
		this.realmId = realmId;
		this.identityProviders = identityProviders;
	}

	public Map<String, IdentityProviderModel> getIdentityProviders() {
		return identityProviders;
	}

	public String getRealmId() {
		return realmId;
	}

	public static class ExternalizerImpl implements Externalizer<CachedIdentityProviders> {

        @Override
        public void writeObject(ObjectOutput output, CachedIdentityProviders obj) throws IOException {
            MarshallUtil.marshallString(obj.realmId, output);
            MarshallUtil.marshallMap(obj.identityProviders, output);
        }

        @Override
        public CachedIdentityProviders readObject(ObjectInput input) throws IOException, ClassNotFoundException {
            String realmId = MarshallUtil.unmarshallString(input);
            Map<String, IdentityProviderModel> identityProviders = MarshallUtil.unmarshallMap(input, new MarshallUtil.MapBuilder<String, IdentityProviderModel, Map<String, IdentityProviderModel>>() {
                @Override
                public Map<String, IdentityProviderModel> build(int size) {
                    return new HashMap<>(size);
                }
            });
            return new CachedIdentityProviders(realmId, identityProviders);
        }
    }
	
	
}
