package org.keycloak.test.broker.saml.aggregate.metadata;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.keycloak.broker.saml.aggregate.metadata.SAMLAggregateParser;
import org.keycloak.saml.common.exceptions.ParsingException;

public class SAMLAggregateMetadataParsingTest {
  
  public static final String MD_URL = "http://md.idem.garr.it/metadata/edugain2idem-metadata-sha256.xml";
  
  
  @Test
  public void testMetadataParsing() throws IOException, ParsingException {
    
    SAMLAggregateParser parser  = new SAMLAggregateParser();
    
    URL mdUrl = new URL(MD_URL);
    parser.parseMetadata(mdUrl.openStream());
    
  }
  
  

}
