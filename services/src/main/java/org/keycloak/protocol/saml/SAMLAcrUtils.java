package org.keycloak.protocol.saml;

import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.models.AuthenticatedClientSessionModel;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.util.GeneralAcrUtils;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.services.managers.AuthenticationManager;
import org.keycloak.sessions.AuthenticationSessionModel;

import java.util.Iterator;
import java.util.Map;
import java.util.OptionalInt;
import java.util.stream.IntStream;

public class SAMLAcrUtils {

    private final Map<String, Integer> acrLoaMap;

    public SAMLAcrUtils(ClientModel client) {
        this.acrLoaMap = GeneralAcrUtils.getAcrLoaMap(client);
    }

    public void setLoaFromRequestedAuthn(RequestedAuthnContextType requestedAuthn, AuthenticationSessionModel authSession){
//                exact: make the user register with the subflows corresponding to the asked LoAs.
//                minimum: make the user register with the subflow corresponding to the first asked for LoA in the list.
//                maximum: make the user register with all subflows up until and including the subflow corresponding to the highest LoA that was asked for.
//                better: make the user register with the subflow corresponding to the next LoA higher than the highest one asked for.
        AuthnContextComparisonType comparison = requestedAuthn.getComparison() != null ? requestedAuthn.getComparison() : AuthnContextComparisonType.EXACT;
        if (AuthnContextComparisonType.MINIMUM.equals(comparison)) {
            String text = null;
            Integer loa = null;
            for (String classRef : requestedAuthn.getAuthnContextClassRef()) {
                 loa = getLoa(classRef);
                if (loa < Constants.MAXIMUM_LOA) {
                    text = classRef;
                    break;
                }
            }
            authSession.setClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION, String.valueOf(loa));
            authSession.setClientNote(Constants.REQUESTED_AUTHN_CONTEXT_CLASS_REF, text );
        } else {
            IntStream stream = requestedAuthn.getAuthnContextClassRef().stream().mapToInt(this ::getLoa);
            OptionalInt loa = AuthnContextComparisonType.EXACT.equals(comparison) ? stream.min() : stream.max();
            authSession.setClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION, String.valueOf( AuthnContextComparisonType.BETTER.equals(comparison) ? loa.getAsInt() + 1 : loa.getAsInt()));
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
