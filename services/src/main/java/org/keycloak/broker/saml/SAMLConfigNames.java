package org.keycloak.broker.saml;

import org.keycloak.saml.common.util.XmlKeyInfoKeyNameTransformer;

public class SAMLConfigNames {

	public static final XmlKeyInfoKeyNameTransformer DEFAULT_XML_KEY_INFO_KEY_NAME_TRANSFORMER = XmlKeyInfoKeyNameTransformer.NONE;

	public static final String ADD_EXTENSIONS_ELEMENT_WITH_KEY_INFO = "addExtensionsElementWithKeyInfo";
	public static final String BACKCHANNEL_SUPPORTED = "backchannelSupported";
	public static final String ENCRYPTION_PUBLIC_KEY = "encryptionPublicKey";
	public static final String FORCE_AUTHN = "forceAuthn";
	public static final String NAME_ID_POLICY_FORMAT = "nameIDPolicyFormat";
	public static final String POST_BINDING_AUTHN_REQUEST = "postBindingAuthnRequest";
	public static final String POST_BINDING_LOGOUT = "postBindingLogout";
	public static final String POST_BINDING_RESPONSE = "postBindingResponse";
	public static final String SIGNATURE_ALGORITHM = "signatureAlgorithm";
	public static final String SIGNING_CERTIFICATE_KEY = "signingCertificate";
	public static final String SINGLE_LOGOUT_SERVICE_URL = "singleLogoutServiceUrl";
	public static final String SINGLE_SIGN_ON_SERVICE_URL = "singleSignOnServiceUrl";
	public static final String VALIDATE_SIGNATURE = "validateSignature";
	public static final String PRINCIPAL_TYPE = "principalType";
	public static final String PRINCIPAL_ATTRIBUTE = "principalAttribute";
	public static final String WANT_ASSERTIONS_ENCRYPTED = "wantAssertionsEncrypted";
	public static final String WANT_ASSERTIONS_SIGNED = "wantAssertionsSigned";
	public static final String WANT_AUTHN_REQUESTS_SIGNED = "wantAuthnRequestsSigned";
	public static final String XML_SIG_KEY_INFO_KEY_NAME_TRANSFORMER = "xmlSigKeyInfoKeyNameTransformer";

}
