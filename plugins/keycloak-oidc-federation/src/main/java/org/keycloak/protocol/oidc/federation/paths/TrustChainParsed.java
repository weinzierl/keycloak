package org.keycloak.protocol.oidc.federation.paths;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.keycloak.protocol.oidc.federation.beans.EntityStatement;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 *  Parsed trustChain. Contains only the payload of the jwt-like structure
 *  We assume that a TrustChainRaw has at position 0 the leaf node and position (size-1) the trust anchor 
 */
public class TrustChainParsed extends ArrayList<EntityStatement>{
	
}

