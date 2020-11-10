package org.keycloak.protocol.oidc.federation.configuration;

import java.io.File;
import java.io.IOException;

import org.keycloak.protocol.oidc.federation.configuration.beans.OIDCFederationConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Config {

	
	private static OIDCFederationConfig config;
	
	static {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
		ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
		File file = new File(classLoader.getResource("federation-config.yml").getFile());
		
		try {
			config = om.readValue(file, OIDCFederationConfig.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static OIDCFederationConfig getConfig() {
		return config;
	}
	
}
