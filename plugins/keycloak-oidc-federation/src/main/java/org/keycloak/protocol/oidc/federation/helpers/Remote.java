package org.keycloak.protocol.oidc.federation.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;

public class Remote {

	
	
	public static String getContentFrom(URL url) throws IOException {
		StringBuffer content = new StringBuffer();
		HttpURLConnection con = (HttpURLConnection) url.openConnection();
		con.setRequestMethod("GET");
		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		while ((inputLine = in.readLine()) != null)
		    content.append(inputLine);
		return content.toString();
	}
	
	
}
