package org.keycloak.testsuite.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.NotFoundException;
import javax.ws.rs.core.Response;

import org.jboss.logging.Logger;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.keycloak.admin.client.resource.IdentityProviderResource;
import org.keycloak.common.util.StreamUtil;
import org.keycloak.events.admin.OperationType;
import org.keycloak.events.admin.ResourceType;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.IdentityProvidersFederationRepresentation;
import org.keycloak.testsuite.Assert;
import org.keycloak.testsuite.util.AdminEventPaths;

import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

public class IdentityProvidersFederationTest extends AbstractAdminTest {

	private static Undertow SERVER;

	private static final Logger log = Logger.getLogger(IdentityProvidersFederationTest.class);

	private static final Set<String> aliasSet = new HashSet<>(
			Arrays.asList(new String[] { "6b6b716bef3c495083e31e1a71e8622e07d69b955cc3d9764fe28be5d0e8fb02",
					"00092d0295bee88b7b381b7c662cb0cc5919fe2d37b29896fa59923e107afda1","5168734e074c0bd8e432066851abed4a6b34f1d291b6ae8e8d0f163a71e48983" }));
	private static final String entityIdIdP = "https://idp.rash.al/simplesaml/saml2/idp/metadata.php";
	private static final String hashEntityIdIdP = "5168734e074c0bd8e432066851abed4a6b34f1d291b6ae8e8d0f163a71e48983" ;
	private static final String authority = "http://aai.grnet.gr/";
	private static final String attributeName="http://macedir.org/entity-category-support";
	private static final String attributeValue="http://clarin.eu/category/clarin-member";

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
    public void testCreateUpdateAndRemove() {

        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", new HashSet<>(),
            new HashSet<>(), new HashSet<>(), new HashSet<>(), new HashMap<>());

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
        assertEquals(3, representation.getIdentityprovidersAlias().size());
        representation.getIdentityprovidersAlias().stream().forEach(idpAlias -> {
            assertTrue("wrong IdPs", aliasSet.contains(idpAlias));
            // find idp and check parameters
            IdentityProviderResource provider = realm.identityProviders().get(idpAlias);
            IdentityProviderRepresentation idp = provider.toRepresentation();
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
            assertTrue("IdP postBindingResponse not exist", idp.getConfig().containsKey("postBindingResponse"));
            assertTrue( Boolean.valueOf(idp.getConfig().get("postBindingResponse")));
            //change postBindingResponse to false
            idp.getConfig().put("postBindingResponse", "false");
            IdentityProviderResource identityProviderResource = realm.identityProviders().get(idp.getAlias());
            identityProviderResource.update(idp);
            idp = identityProviderResource.toRepresentation();
            assertFalse( Boolean.valueOf(idp.getConfig().get("postBindingResponse")));
        });

        //update federation in order to update idps based on xml
        representation.setUpdateFrequencyInMins(1);
        Response response = realm.identityProvidersFederation().create(representation);
        sleep(90000);
         //postBindingResponse must be true again
        representation = realm.identityProvidersFederation()
            .getIdentityProviderFederation(internalId);
        assertNotNull(representation);
        representation.getIdentityprovidersAlias().stream().forEach(idpAlias -> {
            IdentityProviderResource provider = realm.identityProviders().get(idpAlias);
            IdentityProviderRepresentation idp = provider.toRepresentation();
            assertTrue("IdP postBindingResponse not exist", idp.getConfig().containsKey("postBindingResponse"));
            assertTrue( Boolean.valueOf(idp.getConfig().get("postBindingResponse")));
        });

        removeFederation(representation.getInternalId());
    }

	@Test
	public void testCreateWithBlackListandRemove() {
	    //blacklist entityIdIdP entity id and registrationAuthority="http://aai.grnet.gr/"

		//create with excluding one idp
		Set<String> entityIdBlackList = new HashSet<>();
		entityIdBlackList.add(entityIdIdP);
		Set<String> authorityBlackList = new HashSet<>();
		authorityBlackList.add(authority);
		String internalId = createFederation("edugain-sample",
				"http://localhost:8880/edugain-sample-test.xml",entityIdBlackList,new HashSet<>(),authorityBlackList,new HashSet<>(), new HashMap<>());

		sleep(90000);
		// first execute trigger update idps and then get identity providers federation
		IdentityProvidersFederationRepresentation representation = realm.identityProvidersFederation()
				.getIdentityProviderFederation(internalId);
		assertNotNull(representation);

		assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
		assertEquals("not saml federation", "saml", representation.getProviderId());
		assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
				representation.getUrl());

		// must be one idp
		assertEquals(1, representation.getIdentityprovidersAlias().size());
        representation.getIdentityprovidersAlias().stream().forEach(idpAlias -> {
            assertEquals("wrong IdP", hashEntityIdIdP, idpAlias);
            // find idp and check parameters
            IdentityProviderResource provider = realm.identityProviders().get(idpAlias);
            IdentityProviderRepresentation idp = provider.toRepresentation();
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
            assertTrue("IdP postBindingResponse not exist", idp.getConfig().containsKey("postBindingResponse"));
            assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingResponse")));
        });

		removeFederation(representation.getInternalId());
	}
	
	@Test
    public void testCreateWithWhiteListandRemove() {
	  //whitelist entityIdIdP entity id and registrationAuthority="http://aai.grnet.gr/"
	    
        //create with excluding one idp
        Set<String> entityIdWhiteList = new HashSet<>();
        entityIdWhiteList.add(entityIdIdP);
        Set<String> authorityWhiteList = new HashSet<>();
        authorityWhiteList.add(authority);
        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", new HashSet<>(),
            entityIdWhiteList, new HashSet<>(), authorityWhiteList, new HashMap<>());

        sleep(90000);
        // first execute trigger update idps and then get identity providers federation
        IdentityProvidersFederationRepresentation representation = realm.identityProvidersFederation()
                .getIdentityProviderFederation(internalId);
        assertNotNull(representation);

        assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
        assertEquals("not saml federation", "saml", representation.getProviderId());
        assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                representation.getUrl());

        // must be two saml idps and not contain
        assertEquals(2, representation.getIdentityprovidersAlias().size());
        assertFalse("wrong IdPs", representation.getIdentityprovidersAlias().contains(hashEntityIdIdP));
        representation.getIdentityprovidersAlias().stream().forEach(idpAlias -> {
            assertTrue("wrong IdPs", aliasSet.contains(idpAlias));
            // find idp and check parameters
            IdentityProviderResource provider = realm.identityProviders().get(idpAlias);
            IdentityProviderRepresentation idp = provider.toRepresentation();
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
            assertTrue("IdP postBindingResponse not exist", idp.getConfig().containsKey("postBindingResponse"));
            assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingResponse")));
        });

        removeFederation(representation.getInternalId());
    }
	
	@Test
    public void testCreateWithCategoryBlackListandRemove() {
      //blacklist with attributeName and attributeValue exclude only hashEntityIdIdP
        
        Map<String,List<String>> blackMap =  new HashMap<>();
        blackMap.put(attributeName, Stream.of(attributeValue).collect(Collectors.toList()));
        String internalId = createFederation("edugain-sample", "http://localhost:8880/edugain-sample-test.xml", new HashSet<>(),
            new HashSet<>(), new HashSet<>(), new HashSet<>(), blackMap);

        sleep(90000);
        // first execute trigger update idps and then get identity providers federation
        IdentityProvidersFederationRepresentation representation = realm.identityProvidersFederation()
                .getIdentityProviderFederation(internalId);
        assertNotNull(representation);

        assertEquals("wrong federation alias", "edugain-sample", representation.getAlias());
        assertEquals("not saml federation", "saml", representation.getProviderId());
        assertEquals("wrong url", "http://localhost:8880/edugain-sample-test.xml",
                representation.getUrl());

        // must be two saml idps and not contain
        assertEquals(2, representation.getIdentityprovidersAlias().size());
        assertFalse("wrong IdPs", representation.getIdentityprovidersAlias().contains(hashEntityIdIdP));
        representation.getIdentityprovidersAlias().stream().forEach(idpAlias -> {
            assertTrue("wrong IdPs", aliasSet.contains(idpAlias));
            // find idp and check parameters
            IdentityProviderResource provider = realm.identityProviders().get(idpAlias);
            IdentityProviderRepresentation idp = provider.toRepresentation();
            assertEquals("not saml IdP", "saml", idp.getProviderId());
            assertNotNull("empty IdP config", idp.getConfig());
            assertTrue("IdP singleSignOnServiceUrl not exist", idp.getConfig().containsKey("singleSignOnServiceUrl"));
            assertTrue("IdP postBindingResponse not exist", idp.getConfig().containsKey("postBindingResponse"));
            assertTrue(Boolean.valueOf(idp.getConfig().get("postBindingResponse")));
        });

        removeFederation(representation.getInternalId());
    }



    private String createFederation(String alias, String url, Set<String> blackList, Set<String> whitelist,
        Set<String> registrationAuthorityBlackList, Set<String> registrationAuthorityWhitelist, Map<String,List<String>> categoryBlackList) {
        IdentityProvidersFederationRepresentation representation = new IdentityProvidersFederationRepresentation();
        representation.setAlias(alias);
        representation.setProviderId("saml");
        representation.setUpdateFrequencyInMins(60);
        representation.setUrl(url);
        representation.setEntityIdBlackList(blackList);
        representation.setEntityIdWhiteList(whitelist);
        representation.setRegistrationAuthorityBlackList(registrationAuthorityBlackList);
        representation.setRegistrationAuthorityWhiteList(registrationAuthorityWhitelist);
        representation.setCategoryBlackList(categoryBlackList);

        Response response = realm.identityProvidersFederation().create(representation);
        String id = ApiUtil.getCreatedId(response);
        Assert.assertNotNull(id);
        response.close();

        // may not need all this implementation
        getCleanup().addIdentityProviderFederationId(id);

        assertAdminEvents.assertEvent(realmId, OperationType.CREATE, AdminEventPaths.identityProvidersFederationPath(alias),
            representation, ResourceType.IDENTITY_PROVIDERS_FEDERATION);

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
