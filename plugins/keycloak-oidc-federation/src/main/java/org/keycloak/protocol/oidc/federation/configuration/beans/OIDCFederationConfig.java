package org.keycloak.protocol.oidc.federation.configuration.beans;

import java.util.List;

public class OIDCFederationConfig {

	private List<String> authorityHints;
	private List<String> trustAnchors;
	

	public List<String> getAuthorityHints() {
		return authorityHints;
	}

	public void setAuthorityHints(List<String> authorityHints) {
		this.authorityHints = authorityHints;
	}

	public List<String> getTrustAnchors() {
		return trustAnchors;
	}

	public void setTrustAnchors(List<String> trustAnchors) {
		this.trustAnchors = trustAnchors;
	}
	
}
