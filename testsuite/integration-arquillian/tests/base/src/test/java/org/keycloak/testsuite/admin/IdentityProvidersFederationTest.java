package org.keycloak.testsuite.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.apache.commons.lang3.StringUtils;
import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.models.sessions.infinispan.changes.sessions.CrossDCLastSessionRefreshStoreFactory;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;
import org.keycloak.timer.TimerProvider;
import org.keycloak.timer.TimerProvider.TimerTaskContext;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class IdentityProvidersFederationTest extends AbstractAdminTest {
	
	private static Undertow SERVER;

	private static final Logger log = Logger.getLogger(IdentityProvidersFederationTest.class);

	private static final Set<String> aliasSet = new HashSet<>(
			Arrays.asList(new String[] { "6b6b716bef3c495083e31e1a71e8622e07d69b955cc3d9764fe28be5d0e8fb02",
					"00092d0295bee88b7b381b7c662cb0cc5919fe2d37b29896fa59923e107afda1" }));
	private static final String excludeIdp = "https://idp.rash.al/simplesaml/saml2/idp/metadata.php";
	private static final String aliasIdp = "6b6b716bef3c495083e31e1a71e8622e07d69b955cc3d9764fe28be5d0e8fb02";
	
	@BeforeClass
	public static void onBeforeClass() {
		SERVER = Undertow.builder().addHttpListener(8880, "localhost", new HttpHandler() {
			@Override
			public void handleRequest(HttpServerExchange exchange) throws Exception {
				writeResponse(exchange.getRequestURI(), exchange);
			}

			private void writeResponse(String file, HttpServerExchange exchange) throws IOException {
				exchange.getResponseSender().send(
						StreamUtil.readString(getClass().getResourceAsStream("/federation/saml" + file), Charset.defaultCharset()));
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
	public void testCreateAndRemove() {

		String internalId = createFederation("edugain-sample",
				"http://localhost:8880/edugain-sample-test.xml", new HashSet<>());

		sleep(90000);
		// first execute trigger update idps and then get identity providers federation
		IdentityProvidersFederationRepresentation representation = realm.identityProvidersFederation()
				.getIdentityProviderFederation(internalId);
		assertNotNull(representation);

		assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
		assertEquals("not saml federation", "saml", representation.getProviderId());
		assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
				representation.getUrl());

		// must be two saml idps with alias1 and alias2
		assertEquals(2, representation.getIdentityprovidersAlias().size());
		representation.getIdentityprovidersAlias().stream().forEach(idpAlias -> {
			assertTrue("wrong IdPs", aliasSet.contains(idpAlias));
			// find idp and check parameters
			IdentityProviderResource provider = realm.identityProviders().get(idpAlias);
			IdentityProviderRepresentation idp = provider.toRepresentation();
			assertEquals("not saml IdP", "saml", idp.getProviderId());
			assertNotNull("empty IdP config", idp.getConfig());
			assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
		});

		removeFederation(representation.getInternalId());
	}
	
	@Test
	public void testCreateWithExcludeListandRemove() {

		//create with excluding one idp
		Set<String> skipIdPs = new HashSet<>();
		skipIdPs.add(excludeIdp);
		String internalId = createFederation("edugain-sample",
				"http://localhost:8880/edugain-sample-test.xml",skipIdPs);

		sleep(90000);
		// first execute trigger update idps and then get identity providers federation
		IdentityProvidersFederationRepresentation representation = realm.identityProvidersFederation()
				.getIdentityProviderFederation(internalId);
		assertNotNull(representation);

		assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
		assertEquals("not saml federation", "saml", representation.getProviderId());
		assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
				representation.getUrl());

		// must be two saml idps with alias1 and alias2
		assertEquals(1, representation.getIdentityprovidersAlias().size());
		representation.getIdentityprovidersAlias().stream().forEach(idpAlias -> {
			assertEquals("wrong IdP", aliasIdp, idpAlias);
			// find idp and check parameters
			IdentityProviderResource provider = realm.identityProviders().get(idpAlias);
			IdentityProviderRepresentation idp = provider.toRepresentation();
			assertEquals("not saml IdP", "saml", idp.getProviderId());
			assertNotNull("empty IdP config", idp.getConfig());
			assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
		});

		removeFederation(representation.getInternalId());
	}


	private String createFederation(String alias, String url,Set<String> skipIdps) {
		IdentityProvidersFederationRepresentation representation = new IdentityProvidersFederationRepresentation();
		representation.setAlias(alias);
		representation.setProviderId("saml");
		representation.setRefreshEveryMinutes(60);
		representation.setUrl(url);
		representation.setSkipIdps(skipIdps);

		Response response = realm.identityProvidersFederation().create(representation);
		String id = ApiUtil.getCreatedId(response);
		Assert.assertNotNull(id);
		response.close();

		// may not need all this implementation
		getCleanup().addIdentityProviderFederationId(id);

		assertAdminEvents.assertEvent(realmId, OperationType.CREATE,
				AdminEventPaths.identityProvidersFederationPath(alias), representation,
				ResourceType.IDENTITY_PROVIDERS_FEDERATION);

		return id;
	}

	private void removeFederation(String id) {
		realm.identityProvidersFederation().delete(id);

		// federation and its idps must be deleted
		try {
			realm.identityProvidersFederation().getIdentityProviderFederation(id);
			Assert.fail("Not expected to found federation");

		} catch (NotFoundException nfe) {
			// Expected
		}

		aliasSet.stream().forEach(idp -> {
			try {
				IdentityProviderResource resource = realm.identityProviders().get(idp);
				IdentityProviderRepresentation idpRes = resource.toRepresentation();
				Assert.fail("Not expected to found idp");

			} catch (NotFoundException nfe) {
				// Expected
			}
		});

	}

	private static void sleep(long ms) {
		try {
			log.infof("Sleeping for %d ms", ms);
			Thread.sleep(ms);
		} catch (InterruptedException ie) {
			throw new RuntimeException(ie);
		}
	}

}
