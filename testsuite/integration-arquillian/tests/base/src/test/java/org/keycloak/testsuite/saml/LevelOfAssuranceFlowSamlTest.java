package org.keycloak.testsuite.saml;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.ws.rs.core.Response;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.keycloak.authentication.authenticators.browser.PasswordFormFactory;
import org.keycloak.authentication.authenticators.browser.UsernameFormFactory;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticator;
import org.keycloak.authentication.authenticators.conditional.ConditionalLoaAuthenticatorFactory;
import org.keycloak.dom.saml.v2.protocol.AuthnContextComparisonType;
import org.keycloak.dom.saml.v2.protocol.AuthnRequestType;
import org.keycloak.models.AuthenticationExecutionModel;
import org.keycloak.models.Constants;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.saml.common.exceptions.ConfigurationException;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.exceptions.ProcessingException;
import org.keycloak.saml.processing.api.saml.v2.request.SAML2Request;
import org.keycloak.saml.processing.core.saml.v2.common.SAMLDocumentHolder;
import org.keycloak.testsuite.util.ClientBuilder;
import org.keycloak.testsuite.util.FlowUtil;
import org.keycloak.testsuite.util.SamlClient;
import org.keycloak.testsuite.util.SamlClientBuilder;
import org.keycloak.testsuite.utils.io.IOUtil;
import org.keycloak.util.JsonSerialization;
import org.w3c.dom.Document;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

public class LevelOfAssuranceFlowSamlTest extends AbstractSamlTest {

    private static final String FIRST_LEVEL_LOA = "https://refeds.org/profile/sfa";
    private static final String SECOND_LEVEL_LOA = "https://refeds.org/profile/mfa";
    private static final String DEFAULT_FIRST_LOA = "https://refeds.org/profile/1";

    @Before
    public void setupFlow() {

        final String newFlowAlias = "browser -  Level of Authebtication FLow";
        testingClient.server(REALM_NAME).run(session -> FlowUtil.inCurrentRealm(session).copyBrowserFlow(newFlowAlias));
        testingClient.server(REALM_NAME)
                .run(session -> FlowUtil.inCurrentRealm(session).selectFlow(newFlowAlias).inForms(forms -> forms.clear()
                        // level 1 authentication
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "1");
                                        config.getConfig().put(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION, "true");
                                    });

                            // username input for level 2
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, UsernameFormFactory.PROVIDER_ID);

                        })

                        // level 2 authentication
                        .addSubFlowExecution(AuthenticationExecutionModel.Requirement.CONDITIONAL, subFlow -> {
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, ConditionalLoaAuthenticatorFactory.PROVIDER_ID,
                                    config -> {
                                        config.getConfig().put(ConditionalLoaAuthenticator.LEVEL, "2");
                                        config.getConfig().put(ConditionalLoaAuthenticator.STORE_IN_USER_SESSION, "true");
                                    });

                            // password required for level 2
                            subFlow.addAuthenticatorExecution(AuthenticationExecutionModel.Requirement.REQUIRED, PasswordFormFactory.PROVIDER_ID);

                        })


                ).defineAsBrowserFlow());
    }

    @Test
    public void testExactAuthnValue() throws ParsingException, ConfigurationException, ProcessingException, IOException, URISyntaxException {
        ClientRepresentation client = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);
        adminClient.realm(REALM_NAME)
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client).attribute(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(Stream.of(new AbstractMap.SimpleEntry<>(SECOND_LEVEL_LOA, "2")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))).build());

        SAMLDocumentHolder document = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST, AuthnContextComparisonType.EXACT, Stream.of(SECOND_LEVEL_LOA).collect(Collectors.toList()))
                .build()
                .login().user(bburkeUser).build().login().user(bburkeUser).build()
                .getSamlResponse(SamlClient.Binding.POST);

        String samlDocumentString = IOUtil.documentToString(document.getSamlDocument());
        assertThat(samlDocumentString, CoreMatchers.containsString("<saml:AuthnContextClassRef>" + SECOND_LEVEL_LOA + "</saml:AuthnContextClassRef>"));


    }


    @Test
    public void testExactAuthnValueWithoutSettingToClient() throws IOException, ParsingException, ConfigurationException, ProcessingException {
        ClientRepresentation client = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);
     //   client.getAttributes().remove(Constants.ACR_LOA_MAP);
        adminClient.realm(REALM_NAME)
                .clients()
                .get(client.getId())
                .update( ClientBuilder.edit(client).attribute(Constants.ACR_LOA_MAP, null).build());

        AuthnRequestType loginRep = SamlClient.createLoginRequestDocument(SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, null, AuthnContextComparisonType.EXACT, Stream.of(SECOND_LEVEL_LOA).collect(Collectors.toList()));
        Document doc = SAML2Request.convert(loginRep);

        HttpUriRequest post = SamlClient.Binding.POST.createSamlUnsignedRequest(getAuthServerSamlEndpoint(REALM_NAME), null, doc);

        try (CloseableHttpClient httpClient = HttpClientBuilder.create().setRedirectStrategy(new SamlClient.RedirectStrategyWithSwitchableFollowRedirect()).build();
             CloseableHttpResponse response = httpClient.execute(post)) {
            Assert.assertEquals(response.getStatusLine().getStatusCode(), 400);
            assertThat(EntityUtils.toString(response.getEntity(), "UTF-8"), containsString("Unsupported Authentication Contexts"));

            response.close();
        }

    }

    @Test
    public void testMinimumAuthnValueWithoutSettingToClient() throws URISyntaxException {

        ClientRepresentation client = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);
        //   client.getAttributes().remove(Constants.ACR_LOA_MAP);
        adminClient.realm(REALM_NAME)
                .clients()
                .get(client.getId())
                .update( ClientBuilder.edit(client).attribute(Constants.ACR_LOA_MAP, null).build());


        SAMLDocumentHolder document = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST, AuthnContextComparisonType.MINIMUM, Stream.of(SECOND_LEVEL_LOA).collect(Collectors.toList()))
                .build()
                .login().user(bburkeUser).build()
                .getSamlResponse(SamlClient.Binding.POST);

        String samlDocumentString = IOUtil.documentToString(document.getSamlDocument());
        assertThat(samlDocumentString, CoreMatchers.containsString("<saml:AuthnContextClassRef>" + DEFAULT_FIRST_LOA + "</saml:AuthnContextClassRef>"));
    }

    @Test
    public void testMaximumAuthnValue() throws IOException, URISyntaxException {
        ClientRepresentation client = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);
        adminClient.realm(REALM_NAME)
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client).attribute(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(Stream.of(new AbstractMap.SimpleEntry<>(FIRST_LEVEL_LOA, "1"), new AbstractMap.SimpleEntry<>(SECOND_LEVEL_LOA, "2")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))).build());

        SAMLDocumentHolder document = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST, AuthnContextComparisonType.MAXIMUM, Stream.of(FIRST_LEVEL_LOA, SECOND_LEVEL_LOA).collect(Collectors.toList()))
                .build()
                .login().user(bburkeUser).build().login().user(bburkeUser).build()
                .getSamlResponse(SamlClient.Binding.POST);

        String samlDocumentString = IOUtil.documentToString(document.getSamlDocument());
        assertThat(samlDocumentString, CoreMatchers.containsString("<saml:AuthnContextClassRef>" + SECOND_LEVEL_LOA + "</saml:AuthnContextClassRef>"));
    }

    @Test
    public void testBetterAuthnValue() throws ParsingException, ConfigurationException, ProcessingException, IOException, URISyntaxException {
        ClientRepresentation client = adminClient.realm(REALM_NAME)
                .clients()
                .findByClientId(SAML_CLIENT_ID_SALES_POST)
                .get(0);
        adminClient.realm(REALM_NAME)
                .clients()
                .get(client.getId())
                .update(ClientBuilder.edit(client).attribute(Constants.ACR_LOA_MAP, JsonSerialization.writeValueAsString(Stream.of(new AbstractMap.SimpleEntry<>(FIRST_LEVEL_LOA, "1"),new AbstractMap.SimpleEntry<>(SECOND_LEVEL_LOA, "2")).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)))).build());

        SAMLDocumentHolder document = new SamlClientBuilder()
                .authnRequest(getAuthServerSamlEndpoint(REALM_NAME), SAML_CLIENT_ID_SALES_POST, SAML_ASSERTION_CONSUMER_URL_SALES_POST, SamlClient.Binding.POST, AuthnContextComparisonType.BETTER, Stream.of(FIRST_LEVEL_LOA).collect(Collectors.toList()))
                .build()
                .login().user(bburkeUser).build().login().user(bburkeUser).build()
                .getSamlResponse(SamlClient.Binding.POST);

        String samlDocumentString = IOUtil.documentToString(document.getSamlDocument());
        assertThat(samlDocumentString, CoreMatchers.containsString("<saml:AuthnContextClassRef>" + SECOND_LEVEL_LOA + "</saml:AuthnContextClassRef>"));
    }
}
