package net.anthavio.vinbudin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import net.anthavio.httl.auth.OAuth2;
import net.anthavio.httl.auth.OAuth2Builder;

import org.springframework.stereotype.Service;

/**
 * 
 * @author martin.vanek
 *
 */
@Service
public class OAuthService {

	public OAuthService() {
		Properties properties = load("oauth.properties");
		/*
		 * Facebook 
		 * https://developers.facebook.com/docs/facebook-login/manually-build-a-login-flow/v2.1
		 * https://developers.facebook.com/apps/
		 */

		//HttlSender facebookSender = HttlSender.url("https://graph.facebook.com").config()
		//		.addHeader("Accept", "application/json").build();
		String clientId = properties.getProperty("facebook.client_id");
		String clientSecret = properties.getProperty("facebook.client_secret");
		String redirectUri = properties.getProperty("facebook.redirect_uri");

		OAuth2 facebook = new OAuth2Builder().setAuthorizationUrl("https://www.facebook.com/dialog/oauth")
				.setClientId(clientId).setClientSecret(clientSecret)
				.setTokenEndpointUrl("https://graph.facebook.com/oauth/access_token").setAuthParam("display", "popup")
				.setRedirectUri(redirectUri).build();
	}

	private Properties load(String name) {
		String property = System.getProperty(name, name);
		try {
			ClassLoader loader = Thread.currentThread().getContextClassLoader();
			InputStream stream = loader.getResourceAsStream(property);
			if (stream == null) {
				File file = new File(property);
				if (file.exists()) {
					stream = new FileInputStream(file);
				} else {
					throw new IllegalArgumentException("Properties resource not found: " + property);
				}
			}
			Properties properties = new Properties();
			properties.load(stream);
			return properties;
		} catch (IOException iox) {
			throw new IllegalStateException("Properties resource " + property + " cannot be loaded", iox);
		}
	}
}
