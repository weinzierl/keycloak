package org.keycloak.protocol.saml;

import java.util.AbstractMap;
import java.util.Map;

import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.util.GeneralAcrUtils;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.sessions.AuthenticationSessionModel;

public class SAMLAcrUtils {

    private final Map<String, Integer> acrLoaMap;

    public SAMLAcrUtils(ClientModel client) {
        this.acrLoaMap = GeneralAcrUtils.getAcrLoaMap(client);
    }

    public void setLoaFromRequestedAuthn(RequestedAuthnContextType requestedAuthn, AuthenticationSessionModel authSession) throws ProcessingException {
//                exact: make the user register with the subflows corresponding to the asked LoAs.
//                minimum: make the user register with the subflow corresponding to the first asked for LoA in the list.
//                maximum: make the user register with all subflows up until and including the subflow corresponding to the highest LoA that was asked for.
//                better: make the user register with the subflow corresponding to the next LoA higher than the highest one asked for.
        AuthnContextComparisonType comparison = requestedAuthn.getComparison() != null ? requestedAuthn.getComparison() : AuthnContextComparisonType.EXACT;
        if (AuthnContextComparisonType.EXACT.equals(comparison) || AuthnContextComparisonType.MINIMUM.equals(comparison)) {
            String text = null;
            Integer loa = null;
            for (String classRef : requestedAuthn.getAuthnContextClassRef()) {
                loa = getLoa(classRef);
                if (loa < Constants.MAXIMUM_LOA) {
                    text = classRef;
                    break;
                }
            }
            if (text == null && AuthnContextComparisonType.EXACT.equals(comparison))
                throw new ProcessingException(Errors.UNSUPPORTED_AUTHENTICATION_CONTEXTS);
            authSession.setClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION, String.valueOf(loa));
            authSession.setClientNote(Constants.REQUESTED_AUTHN_CONTEXT_CLASS_REF, text);
        } else {
            String text = null;
            Integer loa = Constants.MINIMUM_LOA;
            for (String classRef : requestedAuthn.getAuthnContextClassRef()) {
                if (loa < getLoa(classRef)) {
                    loa = getLoa(classRef);
                    text = classRef;
                }
            }
            Integer finalLoa = AuthnContextComparisonType.BETTER.equals(comparison) && loa != Constants.MAXIMUM_LOA ? loa + 1 : loa;
            authSession.setClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION, String.valueOf(finalLoa));
            authSession.setClientNote(Constants.REQUESTED_AUTHN_CONTEXT_CLASS_REF, AuthnContextComparisonType.BETTER.equals(comparison) ? acrLoaMap.entrySet().stream().filter(entry -> entry.getValue() == finalLoa).findAny().orElse(new AbstractMap.SimpleEntry<String, Integer>(null, 0)).getKey() : text);
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
