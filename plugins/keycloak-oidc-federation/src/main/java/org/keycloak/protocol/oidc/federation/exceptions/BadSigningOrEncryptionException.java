package org.keycloak.protocol.oidc.federation.exceptions;

public class BadSigningOrEncryptionException extends Exception {

	public BadSigningOrEncryptionException(String message) {
		super(message);
	}
	
}
