package org.keycloak.testsuite.broker;

public class KcSamlAggregateBrokerTest extends AbstractBrokerTest {

    @Override
    protected BrokerConfiguration getBrokerConfiguration() {
        return KcSamlAggregateBrokerConfiguration.INSTANCE;
    }
}
