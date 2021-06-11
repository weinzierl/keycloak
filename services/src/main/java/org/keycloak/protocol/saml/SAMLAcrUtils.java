package org.keycloak.protocol.saml;

import java.util.AbstractMap;
import java.util.Map;

import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.RequestedAuthnContextType;
import org.keycloak.events.Errors;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.util.GeneralAcrUtils;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.sessions.AuthenticationSessionModel;

public class SAMLAcrUtils {

    private Map<String, Integer> acrLoaMap;

    public SAMLAcrUtils() {
    }

    public SAMLAcrUtils(ClientModel client) {
        this.acrLoaMap = GeneralAcrUtils.getAcrLoaMap(client);
    }

    public void setLoaFromRequestedAuthn(RequestedAuthnContextType requestedAuthn, AuthenticationSessionModel authSession) throws ProcessingException {
//                exact: make the user register with the subflows corresponding to the asked LoAs.
//                minimum: make the user register with the subflow corresponding to the first asked for LoA in the list.
//                maximum: make the user register with all subflows up until and including the subflow corresponding to the highest LoA that was asked for.
//                better: make the user register with the subflow corresponding to the next LoA higher than the highest one asked for.
        AuthnContextComparisonType comparison = requestedAuthn.getComparison() != null ? requestedAuthn.getComparison() : AuthnContextComparisonType.EXACT;
        authSession.setClientNote(Constants.SAML_COMPARISON_TYPE, comparison.name());
        if (AuthnContextComparisonType.EXACT.equals(comparison) || AuthnContextComparisonType.MINIMUM.equals(comparison)) {
            String text = null;
            Integer loa = null;
            for (String classRef : requestedAuthn.getAuthnContextClassRef()) {
                loa = getLoa(classRef);
                if (loa != null) {
                    text = classRef;
                    break;
                }
            }
            if (loa == null && AuthnContextComparisonType.EXACT.equals(comparison)) {
                throw new ProcessingException(Errors.UNSUPPORTED_AUTHENTICATION_CONTEXTS);
            } else if (AuthnContextComparisonType.EXACT.equals(comparison)) {
                authSession.setClientNote(Constants.REQUESTED_AUTHN_CONTEXT_CLASS_REF, text);
            }
            authSession.setClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION, String.valueOf(loa == null ? Constants.DEFAULT_LOA : loa));
        } else {
            requestedAuthn.getAuthnContextClassRef().stream().mapToInt(this::getLoa).max().ifPresent(loa -> authSession.setClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION, String.valueOf(AuthnContextComparisonType.MAXIMUM.equals(comparison) || loa ==  Constants.MINIMUM_LOA ? loa : loa + 1)));
        }
    }

    public String getLoaAuthedicated(AuthenticationSessionModel authSession,ClientModel client) {
        //for unknown loa - assume return loa number
        if ( authSession.getClientNote(Constants.REQUESTED_AUTHN_CONTEXT_CLASS_REF) != null ) {
            //for exact return actual requested flow
            return authSession.getClientNote(Constants.REQUESTED_AUTHN_CONTEXT_CLASS_REF);
        } else if ( authSession.getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION) == null || authSession.getClientNote(Constants.LEVEL_OF_AUTHENTICATION) == null ) {
            return JBossSAMLURIConstants.AC_UNSPECIFIED.get();
        } else  {
            //for maximum return a loa not higher that requested
            //for minimum and exact return any known loa
            Integer loa =AuthnContextComparisonType.MAXIMUM.name().equals(authSession.getClientNote(Constants.SAML_COMPARISON_TYPE) ) && Integer.valueOf(authSession.getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION)) < Integer.valueOf(authSession.getClientNote(Constants.LEVEL_OF_AUTHENTICATION)) ?Integer.valueOf(authSession.getClientNote(Constants.REQUESTED_LEVEL_OF_AUTHENTICATION))  :Integer.valueOf(authSession.getClientNote(Constants.LEVEL_OF_AUTHENTICATION)) ;
            this.acrLoaMap = GeneralAcrUtils.getAcrLoaMap(client);
            return acrLoaMap.entrySet().stream()
                    .filter(e -> e.getValue() == loa)
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse(loa.toString());
        }

    }

    private Integer getLoa(String acr) {
        {
            Integer loa = acrLoaMap.get(acr);
            // for unknown acr, return MINIMUM_LOA
            return loa == null ? Constants.MINIMUM_LOA : loa;
        }
    }
}
