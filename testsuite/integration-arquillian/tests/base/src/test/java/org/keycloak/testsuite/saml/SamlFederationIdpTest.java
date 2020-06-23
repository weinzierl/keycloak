package org.keycloak.testsuite.saml;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.keycloak.testsuite.util.Matchers.bodyHC;
import static org.keycloak.testsuite.util.Matchers.isSamlStatusResponse;
import static org.keycloak.testsuite.util.Matchers.statusCodeIsHC;
import static org.keycloak.testsuite.util.SamlClient.Binding.POST;
import static org.keycloak.testsuite.util.SamlClient.Binding.REDIRECT;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.hamcrest.Matchers;
import org.jboss.logging.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.authentication.authenticators.broker.IdpReviewProfileAuthenticatorFactory;
import org.keycloak.broker.saml.SAMLConfigNames;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.dom.saml.v2.SAML2Object;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType;
import org.keycloak.dom.saml.v2.assertion.AttributeStatementType.ASTChoiceType;
import org.keycloak.dom.saml.v2.assertion.AttributeType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.dom.saml.v2.protocol.NameIDPolicyType;
import org.keycloak.dom.saml.v2.protocol.ResponseType;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.representations.idm.AuthenticationExecutionInfoRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;
import org.keycloak.saml.SAML2LoginResponseBuilder;
import org.keycloak.saml.common.constants.JBossSAMLURIConstants;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.admin.ApiUtil;
import org.keycloak.testsuite.util.SamlClientBuilder;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class SamlFederationIdpTest extends AbstractSamlTest {

	private static Undertow SERVER;
	private static final Logger log = Logger.getLogger(SamlFederationIdpTest.class);
	private static final String loginIdp = "6b6b716bef3c495083e31e1a71e8622e07d69b955cc3d9764fe28be5d0e8fb02";
	private static final String brokerIdp = "00092d0295bee88b7b381b7c662cb0cc5919fe2d37b29896fa59923e107afda1";
	private static final String brokerIdp2 = "5168734e074c0bd8e432066851abed4a6b34f1d291b6ae8e8d0f163a71e48983";

	protected RealmResource realm;
	private String internalId;

	@Before
	public void createFederation() {
		realm = adminClient.realm(REALM_NAME);
		internalId = createFederation("edugain-sample", "http://localhost:8880/sample-federation-authn.xml");
		
	}

	@After
	public void removeFederation() {
		realm.identityProvidersFederation().delete(internalId);
	}

	@BeforeClass
	public static void onBeforeClass() {
		SERVER = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				writeResponse(exchange.getRequestURI(), exchange);
			}

			private void writeResponse(String file, HttpServerExchange exchange) throws IOException {
				exchange.getResponseSender().send(StreamUtil.readString(
						getClass().getResourceAsStream("/federation/saml" + file), Charset.defaultCharset()));
			}
		}).build();

		SERVER.start();
	}

	@AfterClass
	public static void onAfterClass() {
		if (SERVER != null) {
			SERVER.stop();
		}
	}

	@Test
	public void testSamlPostBindingPageIdP() throws Exception {

		new SamlClientBuilder().idpInitiatedLogin(getAuthServerSamlEndpoint(REALM_NAME), "sales-post").build().login()
				.idp(loginIdp).build().execute(r -> {
					Assert.assertThat(r, statusCodeIsHC(Response.Status.OK));
					Assert.assertThat(r,
							bodyHC(allOf(containsString("Redirecting, please wait."),
									containsString("<input type=\"hidden\" name=\"SAMLRequest\""),
									containsString("<h1 id=\"kc-page-title\">"))));
				});

	}

	@Test
	public void testLoginPropagatesToSamlIdentityProvider() throws IOException {

		AuthenticationExecutionInfoRepresentation reviewProfileAuthenticator = null;
		String firstBrokerLoginFlowAlias = null;
		try {

			IdentityProviderRepresentation idpRepresentation = updateIdpByAlias(brokerIdp);
			firstBrokerLoginFlowAlias = idpRepresentation.getFirstBrokerLoginFlowAlias();
			List<AuthenticationExecutionInfoRepresentation> executions = realm.flows()
					.getExecutions(firstBrokerLoginFlowAlias);
			reviewProfileAuthenticator = executions.stream()
					.filter(ex -> Objects.equals(ex.getProviderId(), IdpReviewProfileAuthenticatorFactory.PROVIDER_ID))
					.findFirst().orElseGet(() -> {
						Assert.fail("Could not find update profile in first broker login flow");
						return null;
					});

			reviewProfileAuthenticator.setRequirement(Requirement.DISABLED.name());
			realm.flows().updateExecutions(firstBrokerLoginFlowAlias, reviewProfileAuthenticator);

			SAMLDocumentHolder samlResponse = new SamlClientBuilder()
					.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
							SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
					.transformObject(ar -> {
						NameIDPolicyType nameIDPolicy = new NameIDPolicyType();
						nameIDPolicy.setAllowCreate(Boolean.TRUE);
						nameIDPolicy.setFormat(JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.getUri());

						ar.setNameIDPolicy(nameIDPolicy);
						return ar;
					}).build()

					.login().idp(brokerIdp).build()

					// Virtually perform login at IdP (return artificial SAML response)
					.processSamlResponse(REDIRECT).transformObject(this::createAuthnResponse)
					.targetAttributeSamlResponse().targetUri(getSamlBrokerIdpUrl(REALM_NAME)).build()
					.followOneRedirect() // first-broker-login
					.followOneRedirect() // after-first-broker-login
					.getSamlResponse(POST);

			assertThat(samlResponse.getSamlObject(), isSamlStatusResponse(JBossSAMLURIConstants.STATUS_RESPONDER,
					JBossSAMLURIConstants.STATUS_INVALID_NAMEIDPOLICY));

		} finally {
			reviewProfileAuthenticator.setRequirement(Requirement.REQUIRED.name());
			realm.flows().updateExecutions(firstBrokerLoginFlowAlias, reviewProfileAuthenticator);

		}
	}

	@Test
	public void testRedirectQueryParametersPreserved() throws IOException {

		updateIdpByAlias(brokerIdp2);
		
		// updateIdpForNameIdPolicyFormat(brokerIdp2);
		SAMLDocumentHolder samlResponse = new SamlClientBuilder()
				.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
						SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
				.build().login().idp(brokerIdp2).build()

				// Virtually perform login at IdP (return artificial SAML response)
				.getSamlResponse(REDIRECT);

		assertThat(samlResponse.getSamlObject(), Matchers.instanceOf(AuthnRequestType.class));
		AuthnRequestType ar = (AuthnRequestType) samlResponse.getSamlObject();
		assertThat(ar.getDestination(), Matchers.equalTo(URI.create("https://saml.idp/?service=name&serviceType=prod")));

		Header[] headers = new SamlClientBuilder()
				.authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST,
						SAML_ASSERTION_CONSUMER_URL_SALES_POST, POST)
				.build().login().idp(brokerIdp2).build().doNotFollowRedirects()
				.executeAndTransform(resp -> resp.getHeaders(HttpHeaders.LOCATION));

		assertThat(headers.length, Matchers.is(1));
		assertThat(headers[0].getValue(), Matchers.containsString("https://saml.idp/?service=name&serviceType=prod"));
		assertThat(headers[0].getValue(), Matchers.containsString("SAMLRequest"));

	}


	
	private SAML2Object createAuthnResponse(SAML2Object so) {
		AuthnRequestType req = (AuthnRequestType) so;
		try {
			final ResponseType res = new SAML2LoginResponseBuilder().requestID(req.getID())
					.destination(req.getAssertionConsumerServiceURL().toString()).issuer("https://saml.idp/saml")
					.assertionExpiration(1000000).subjectExpiration(1000000)
					.requestIssuer(getAuthServerRealmBase(REALM_NAME).toString())
					.sessionIndex("idp:" + UUID.randomUUID()).buildModel();

			AttributeStatementType attrStatement = new AttributeStatementType();
			AttributeType attribute = new AttributeType("mail");
			attribute.addAttributeValue("v@w.x");
			attrStatement.addAttribute(new ASTChoiceType(attribute));

			res.getAssertions().get(0).getAssertion().addStatement(attrStatement);

			return res;
		} catch (ConfigurationException | ProcessingException ex) {
			throw new RuntimeException(ex);
		}
	}
	
	  
	private String createFederation(String alias, String url) throws NotFoundException {
		IdentityProvidersFederationRepresentation representation = new IdentityProvidersFederationRepresentation();
		representation.setAlias(alias);
		representation.setProviderId("saml");
		representation.setUpdateFrequencyInMins(60);
		representation.setUrl(url);
		Map<String,String> map = new HashMap<>();
		map.put(SAMLConfigNames.NAME_ID_POLICY_FORMAT, "urn:oasis:names:tc:SAML:2.0:nameid-format:persistent");
		map.put(SAMLConfigNames.WANT_AUTHN_REQUESTS_SIGNED, "false");
		map.put(SAMLConfigNames.WANT_ASSERTIONS_SIGNED, "false");
		map.put(SAMLConfigNames.WANT_ASSERTIONS_ENCRYPTED, "false");
		map.put(SAMLConfigNames.POST_BINDING_AUTHN_REQUEST, "true");
		representation.setConfig(map);

		Response response = realm.identityProvidersFederation().create(representation);
		String id = ApiUtil.getCreatedId(response);
		response.close();

		if (id == null)
			throw new NotFoundException();

		sleep(90000);

		return id;
	}

	private IdentityProviderRepresentation updateIdpByAlias(String idpAlias) {
		IdentityProviderResource identityProviderResource = realm.identityProviders().get(idpAlias);

		IdentityProviderRepresentation representation = identityProviderResource.toRepresentation();

		assertNotNull(representation);
		
		representation.getConfig().put(SAMLConfigNames.NAME_ID_POLICY_FORMAT, JBossSAMLURIConstants.NAMEID_FORMAT_EMAIL.get());
		representation.getConfig().put(SAMLConfigNames.BACKCHANNEL_SUPPORTED, Boolean.FALSE.toString());
		
		identityProviderResource.update(representation);
		return representation;
	}

	private static void sleep(long ms) {
		try {
			log.infof("Sleeping for %d ms", ms);
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}

	protected URI getSamlBrokerIdpUrl(String realmName) {
		return URI.create(getAuthServerRealmBase(realmName).toString() + "/broker/" + brokerIdp + "/endpoint");
	}

}
