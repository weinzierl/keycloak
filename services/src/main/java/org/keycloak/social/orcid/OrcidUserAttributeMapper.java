package org.keycloak.social.orcid;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import org.keycloak.broker.oidc.mappers.AbstractJsonUserAttributeMapper;
import org.keycloak.broker.provider.BrokeredIdentityContext;
import org.keycloak.models.IdentityProviderMapperModel;
import org.keycloak.protocol.oidc.mappers.OIDCAttributeMapperHelper;

public class OrcidUserAttributeMapper extends AbstractJsonUserAttributeMapper {

    public static final String PROVIDER_ID = "orcid-user-attribute-mapper";
    private static final String[] cp = new String[] { OrcidIdentityProviderFactory.PROVIDER_ID };

    @Override
    public String[] getCompatibleProviders() {
        return cp;
    }

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    protected Object getJsonValue(IdentityProviderMapperModel mapperModel, BrokeredIdentityContext context) {

        String jsonField = mapperModel.getConfig().get(CONF_JSON_FIELD);
        if (jsonField == null || jsonField.trim().isEmpty()) {
            logger.warnf("JSON field path is not configured for mapper %s", mapperModel.getName());
            return null;
        }
        jsonField = jsonField.trim();

        if (jsonField.startsWith(JSON_PATH_DELIMITER) || jsonField.endsWith(JSON_PATH_DELIMITER) || jsonField.startsWith("[")) {
            logger.warnf("JSON field path is invalid %s", jsonField);
            return null;
        }

        JsonNode profileJsonNode = (JsonNode) context.getContextData().get(CONTEXT_JSON_NODE);

        Object value = getOrcidJsonValue(profileJsonNode, jsonField);

        if (value == null) {
            logger.debugf("User profile JSON value '%s' is not available.", jsonField);
        }

        return value;
    }

    public Object getOrcidJsonValue(JsonNode baseNode, String fieldPath) {
        logger.debug("Going to process JsonNode path " + fieldPath + " on data " + baseNode);
        if (baseNode != null) {

            List<String> fields = OIDCAttributeMapperHelper.splitClaimPath(fieldPath);
            if (fields.isEmpty() || fieldPath.endsWith(".")) {
                logger.debug("JSON path is invalid " + fieldPath);
                return null;
            }

            JsonNode currentNode = baseNode;
            for (String currentFieldName : fields) {

                // if array path, retrieve field name and index
                String currentNodeName = currentFieldName;
                int arrayIndex = -1;
                if (currentFieldName.endsWith("]")) {
                    int bi = currentFieldName.indexOf("[");
                    if (bi == -1) {
                        logger.debug("Invalid array index construct in " + currentFieldName);
                        return null;
                    }
                    try {
                        String is = currentFieldName.substring(bi + 1, currentFieldName.length() - 1).trim();
                        arrayIndex = Integer.parseInt(is);
                        if( arrayIndex < 0) throw new ArrayIndexOutOfBoundsException();
                    } catch (Exception e) {
                        logger.debug("Invalid array index construct in " + currentFieldName);
                        return null;
                    }
                    currentNodeName = currentFieldName.substring(0, bi).trim();
                }

                currentNode = currentNode.get(currentNodeName);
                if (arrayIndex > -1 && currentNode.isArray()) {
                    logger.debug("Going to take array node at index " + arrayIndex);
                    currentNode = currentNode.get(arrayIndex);
                }

                if (currentNode == null) {
                    logger.debug("JsonNode not found for name " + currentFieldName);
                    return null;
                }

                if (currentNode.isArray()) {
                    List<String> values = new ArrayList<>();
                    for (JsonNode childNode : currentNode) {
                        if (childNode.isTextual()) {
                            values.add(childNode.textValue());
                        } else {
                            String ret = childNode.asText();
                            if (ret != null && !ret.trim().isEmpty())
                                values.add(ret.trim());
                        }
                    }
                    if (values.isEmpty()) {
                        return null;
                    }
                    return values ;
                } else if (currentNode.isNull()) {

                    logger.debug("JsonNode is null node for name " + currentFieldName);
                    return null;
                } else if (currentNode.isValueNode()) {
                    String ret = currentNode.asText();
                    if (ret != null && !ret.trim().isEmpty())
                        return ret.trim();
                    else
                        return null;

                }

            }
            return currentNode;
        }
        return null;
    }

}