package org.keycloak.protocol.oidc.federation.helpers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;

import org.keycloak.crypto.KeyType;
import org.keycloak.crypto.KeyUse;
import org.keycloak.crypto.KeyWrapper;
import org.keycloak.jose.jwk.JSONWebKeySet;
import org.keycloak.jose.jwk.JWK;
import org.keycloak.jose.jwk.JWKBuilder;
import org.keycloak.models.KeycloakSession;

public class FedUtils {

    private static String WELL_KNOWN_SUBPATH = ".well-known/openid-federation";
    
    
    public static String getSelfSignedToken(String issuer) throws MalformedURLException, IOException {
        issuer = issuer.trim();
        if(!issuer.endsWith("/"))
            issuer += "/";
        return getContentFrom(new URL(issuer + WELL_KNOWN_SUBPATH));
    }
    
    public static String getSubordinateToken(String fedApiUrl, String issuer, String subject) throws MalformedURLException, UnsupportedEncodingException, IOException {
        return getContentFrom(new URL(fedApiUrl + "?iss="+urlEncode(issuer)+"&sub="+urlEncode(subject)));
    }
    
    
    private static String getContentFrom(URL url) throws IOException {
        StringBuffer content = new StringBuffer();
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod("GET");
        BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
        String inputLine;
        while ((inputLine = in.readLine()) != null)
            content.append(inputLine);
        return content.toString();
    }
    
    public static JSONWebKeySet getKeySet(KeycloakSession session) {
        List<JWK> keys = new LinkedList<>();
        for (KeyWrapper k : session.keys().getKeys(session.getContext().getRealm())) {
            if (k.getStatus().isEnabled() && k.getUse().equals(KeyUse.SIG) && k.getPublicKey() != null) {
                JWKBuilder b = JWKBuilder.create().kid(k.getKid()).algorithm(k.getAlgorithm());
                if (k.getType().equals(KeyType.RSA)) {
                    keys.add(b.rsa(k.getPublicKey(), k.getCertificate()));
                } else if (k.getType().equals(KeyType.EC)) {
                    keys.add(b.ec(k.getPublicKey()));
                }
            }
        }

        JSONWebKeySet keySet = new JSONWebKeySet();

        JWK[] k = new JWK[keys.size()];
        k = keys.toArray(k);
        keySet.setKeys(k);
        return keySet;
    }
    
    
    private static String urlEncode(String url) throws UnsupportedEncodingException {
        return URLEncoder.encode(url, StandardCharsets.UTF_8.toString());
    }

}