package org.keycloak.protocol.oidc.federation.paths;

import java.util.ArrayList;


/** Raw jwt-like data in this chain. 
 *  We assume that a TrustChainRaw has at position 0 the leaf node and position (size-1) the trust anchor 
 */
public class TrustChainRaw extends ArrayList<String> {

}
