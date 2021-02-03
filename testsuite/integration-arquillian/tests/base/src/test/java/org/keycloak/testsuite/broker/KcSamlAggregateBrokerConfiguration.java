package org.keycloak.testsuite.broker;

import org.keycloak.broker.saml.aggregate.SAMLAggregateIdentityProviderConfig;
import org.keycloak.models.IdentityProviderModel;
import org.keycloak.protocol.ProtocolMapperUtils;
import org.keycloak.protocol.saml.SamlConfigAttributes;
import org.keycloak.protocol.saml.SamlProtocol;
import org.keycloak.protocol.saml.mappers.AttributeStatementHelper;
import org.keycloak.protocol.saml.mappers.UserAttributeStatementMapper;
import org.keycloak.protocol.saml.mappers.UserPropertyAttributeStatementMapper;
import org.keycloak.representations.idm.ClientRepresentation;
import org.keycloak.representations.idm.IdentityProviderRepresentation;
import org.keycloak.representations.idm.ProtocolMapperRepresentation;
import org.keycloak.testsuite.arquillian.SuiteContext;

import java.util.*;

import static org.keycloak.broker.saml.SAMLIdentityProviderConfig.*;
import static org.keycloak.protocol.saml.SamlProtocol.SAML_ASSERTION_CONSUMER_URL_POST_ATTRIBUTE;
import static org.keycloak.testsuite.broker.BrokerTestConstants.*;
import static org.keycloak.testsuite.broker.BrokerTestTools.createIdentityProvider;
import static org.keycloak.testsuite.broker.BrokerTestTools.getAuthRoot;

public class KcSamlAggregateBrokerConfiguration extends KcSamlBrokerConfiguration {

    public static final KcSamlAggregateBrokerConfiguration INSTANCE = new KcSamlAggregateBrokerConfiguration();

    @Override
    public IdentityProviderRepresentation setUpIdentityProvider(SuiteContext suiteContext) {

        IdentityProviderRepresentation idp = createIdentityProvider(IDP_SAML_ALIAS, IDP_SAML_AGGREGATE_PROVIDER_ID);

        idp.setTrustEmail(true);
        idp.setAddReadTokenRoleOnCreate(true);
        idp.setStoreToken(true);

        Map<String, String> config = idp.getConfig();

        config.put(SAMLAggregateIdentityProviderConfig.METADATA_URL,
                getAuthRoot(suiteContext) + "/auth/realms/" + REALM_PROV_NAME + "/protocol/saml/descriptor");

        return idp;
    }
}
