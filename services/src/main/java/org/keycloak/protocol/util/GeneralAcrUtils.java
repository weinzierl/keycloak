package org.keycloak.protocol.util;

import com.fasterxml.jackson.core.type.TypeReference;
import org.jboss.logging.Logger;
import org.keycloak.models.ClientModel;
import org.keycloak.models.Constants;
import org.keycloak.protocol.oidc.utils.AcrUtils;
import org.keycloak.util.JsonSerialization;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class GeneralAcrUtils {

    private static final Logger LOGGER = Logger.getLogger(GeneralAcrUtils.class);

    public static Map<String, Integer> getAcrLoaMap(ClientModel client) {
      String map = client.getAttribute(Constants.ACR_LOA_MAP);
      if (map == null || map.isEmpty()) {
        return Collections.emptyMap();
      }
      try {
        return JsonSerialization.readValue(map, new TypeReference<Map<String, Integer>>() {});
      } catch (IOException e) {
        LOGGER.warn("Invalid client configuration (ACR-LOA map)");
        return Collections.emptyMap();
      }
    }

    public static String mapLoaToAcr(int loa, Map<String, Integer> acrLoaMap, Collection<String> acrValues) {
      String acr = null;
      if (!acrLoaMap.isEmpty() && !acrValues.isEmpty()) {
        int maxLoa = 0;
        for (String requestedAcr : acrValues) {
          Integer mappedLoa = acrLoaMap.get(requestedAcr);
          if (mappedLoa != null && mappedLoa > maxLoa && loa >= mappedLoa) {
            acr = requestedAcr;
            maxLoa = mappedLoa;
          }
        }
      }
      return acr;
    }

   // public static void setRequestedLoa(List<String> acrValues, )
}
