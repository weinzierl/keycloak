package org.keycloak.protocol.oidc.federation.configuration;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.keycloak.protocol.oidc.federation.configuration.beans.OIDCFederationConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

public class Config {

	
	private static OIDCFederationConfig config;
	
	static {
		ObjectMapper om = new ObjectMapper(new YAMLFactory());
	    URL resource = Thread.currentThread().getContextClassLoader().getResource("federation-config.yml");
		
		try {
			config = om.readValue(resource, OIDCFederationConfig.class);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static OIDCFederationConfig getConfig() {
		return config;
	}
	
}
