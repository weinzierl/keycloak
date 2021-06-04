package org.keycloak.protocol.saml;

import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.util.GeneralAcrUtils;

import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class SAMLAcrUtils {

    private final Map<String, Integer> acrLoaMap;

    public SAMLAcrUtils(ClientModel client) {
        this.acrLoaMap = GeneralAcrUtils.getAcrLoaMap(client);
    }

    public Integer getLoaFromRequestedAuthn (RequestedAuthnContextType requestedAuthn){
//                exact: make the user register with the subflows corresponding to the asked LoAs.
//                minimum: make the user register with the subflow corresponding to the first asked for LoA in the list.
//                maximum: make the user register with all subflows up until and including the subflow corresponding to the highest LoA that was asked for.
//                better: make the user register with the subflow corresponding to the next LoA higher than the highest one asked for.
        AuthnContextComparisonType comparison = requestedAuthn.getComparison() != null ? requestedAuthn.getComparison() : AuthnContextComparisonType.EXACT;
        if (AuthnContextComparisonType.MINIMUM.equals(comparison)) {
            return getLoa(requestedAuthn.getAuthnContextClassRef().get(0));
        } else {
            IntStream stream = requestedAuthn.getAuthnContextClassRef().stream().mapToInt(this ::getLoa);
            OptionalInt loa = AuthnContextComparisonType.EXACT.equals(comparison) ? stream.min() : stream.max();
            return AuthnContextComparisonType.BETTER.equals(comparison) ? loa.getAsInt() + 1 : loa.getAsInt();
        }
    }

    private Integer getLoa(String acr) {
        {
            Integer loa = acrLoaMap.get(acr);
            // this is an unknown acr, we assume it is very high
            //is this correct???
            return loa == null ? Constants.MAXIMUM_LOA : loa;
        }
    }
}
