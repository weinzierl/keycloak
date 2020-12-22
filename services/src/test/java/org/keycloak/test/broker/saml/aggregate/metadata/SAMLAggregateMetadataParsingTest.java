package org.keycloak.test.broker.saml.aggregate.metadata;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateParser;
import org.keycloak.saml.common.exceptions.ParsingException;

public class SAMLAggregateMetadataParsingTest {

  public static final String MD_URL =
      "https://gist.githubusercontent.com/enricovianello/5a5a9b4df3709433a3e3a712bc362119/raw/b00d595f6ccf6a17cbc2b1391ba588d80cafe9f4/edugain2idem-metadata-sha256.xml";


  @Test
  public void testMetadataParsing() throws IOException, ParsingException {

    SAMLAggregateParser parser = new SAMLAggregateParser();

    URL mdUrl = new URL(MD_URL);
    parser.parseMetadata(mdUrl.openStream());

  }

}
