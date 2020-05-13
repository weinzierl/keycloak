package org.keycloak.saml.processing.core.parsers.saml.metadata;

import static org.keycloak.saml.processing.core.parsers.saml.metadata.SAMLMetadataQNames.ATTR_LANG;

import java.net.URI;
import java.net.URISyntaxException;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.events.StartElement;

import org.keycloak.dom.saml.v2.metadata.LocalizedNameType;
import org.keycloak.dom.saml.v2.metadata.LocalizedURIType;
import org.keycloak.dom.saml.v2.metadata.UIInfoType;
import org.keycloak.saml.common.exceptions.ParsingException;
import org.keycloak.saml.common.util.StaxParserUtil;

public class SAMLUIInfoParser extends AbstractStaxSamlMetadataParser<UIInfoType> {

    private static final SAMLUIInfoParser INSTANCE = new SAMLUIInfoParser();

    private SAMLUIInfoParser() {
        super(SAMLMetadataQNames.UIINFO);
    }

    public static SAMLUIInfoParser getInstance() {
        return INSTANCE;
    }

    @Override
    protected UIInfoType instantiateElement(XMLEventReader xmlEventReader, StartElement element) throws ParsingException {
    	return new UIInfoType();
    }

    
    @Override
    protected void processSubElement(XMLEventReader xmlEventReader, UIInfoType target, SAMLMetadataQNames element, StartElement elementDetail) throws ParsingException {
        switch (element) {
            case DISPLAY_NAME:
            	LocalizedNameType displayName = new LocalizedNameType(StaxParserUtil.getAttributeValue(elementDetail, ATTR_LANG));
            	StaxParserUtil.advance(xmlEventReader);
            	displayName.setValue(StaxParserUtil.getElementText(xmlEventReader));
            	target.addDisplayName(displayName);
                break;
            case DESCRIPTION:
            	LocalizedNameType description = new LocalizedNameType(StaxParserUtil.getAttributeValue(elementDetail, ATTR_LANG));
            	StaxParserUtil.advance(xmlEventReader);
            	description.setValue(StaxParserUtil.getElementText(xmlEventReader));
            	target.addDescription(description);
                break;
            case KEYWORDS:
            	LocalizedNameType keywords = new LocalizedNameType(StaxParserUtil.getAttributeValue(elementDetail, ATTR_LANG));
            	StaxParserUtil.advance(xmlEventReader);
            	keywords.setValue(StaxParserUtil.getElementText(xmlEventReader));
            	target.addKeywords(keywords);
                break;
            case INFORMATION_URL:
            	LocalizedURIType informationURL = new LocalizedURIType(StaxParserUtil.getAttributeValue(elementDetail, ATTR_LANG));
            	StaxParserUtil.advance(xmlEventReader);
            	try {
            		informationURL.setValue(new URI(StaxParserUtil.getElementText(xmlEventReader)));
            	}catch(URISyntaxException ex) {}
            	target.addInformationURL(informationURL);
                break;
            case PRIVACY_STATEMENT_URL:
            	LocalizedURIType privacyStatementURL = new LocalizedURIType(StaxParserUtil.getAttributeValue(elementDetail, ATTR_LANG));
            	StaxParserUtil.advance(xmlEventReader);
            	try {
            		privacyStatementURL.setValue(new URI(StaxParserUtil.getElementText(xmlEventReader)));
            	}catch(URISyntaxException ex) {}
            	target.addPrivacyStatementURL(privacyStatementURL);
                break;
            case LOGO:
            	LocalizedURIType logo = new LocalizedURIType(StaxParserUtil.getAttributeValue(elementDetail, ATTR_LANG));
            	StaxParserUtil.advance(xmlEventReader);
            	try {
            		logo.setValue(new URI(StaxParserUtil.getElementText(xmlEventReader)));
            	}catch(URISyntaxException ex) {}
            	target.addLogo(logo);
                break;
            default:
                throw LOGGER.parserUnknownTag(StaxParserUtil.getElementName(elementDetail), elementDetail.getLocation());
        }
    }
}