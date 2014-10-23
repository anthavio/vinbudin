package net.anthavio.vinbudin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Properties;

import net.anthavio.httl.HttlSender;
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

	public static enum OAuthProviders {
		GOOGLE, FACEBOOK, GITHUB, LINKEDIN, DISQUS, WOT;

		private OAuth2 oauth;

		private String scopes;

		static OAuthProviders getByName(String provider) {
			OAuthProviders[] values = values();
			for (OAuthProviders value : values) {
				if (value.name().toLowerCase().equals(provider)) {
					return value;
				}
			}
			throw new IllegalArgumentException("No provider: " + provider + " Available: " + Arrays.asList(values));
		}

		public OAuth2 getOAuth() {
			return oauth;
		}

		public String getScopes() {
			return scopes;
		}

		private void setOAuth(OAuth2 oauth, String scopes) {
			if (oauth == null) {
				throw new IllegalArgumentException("Null OAuth2");
			} else if (this.oauth != null) {
				throw new IllegalStateException("OAuth2 already set for " + name());
			}
			this.oauth = oauth;
			if (scopes == null) {
				throw new IllegalArgumentException("Null scopes");
			}
			this.scopes = scopes;
		}
	}

	public OAuthService() {
		Properties properties = load("oauth.properties");
		String redirectUri = properties.getProperty("oauth.redirect_uri");
		OAuthProviders.GOOGLE.setOAuth(buildGoogle(properties, redirectUri), "openid profile");
		OAuthProviders.FACEBOOK.setOAuth(buildFacebook(properties, redirectUri), "public_profile");
		OAuthProviders.LINKEDIN.setOAuth(buildLinkedIn(properties, redirectUri), "r_basicprofile");
		OAuthProviders.GITHUB.setOAuth(buildGithub(properties, redirectUri), "");
		OAuthProviders.DISQUS.setOAuth(buildDisqus(properties, redirectUri), "read");
		OAuthProviders.WOT.setOAuth(buildWot(properties, redirectUri), "");
	}

	private OAuth2 buildWot(Properties properties, String redirectUri) {

		String application_id = properties.getProperty("wot.application_id");
		redirectUri = redirectUri.replace("{provider}", "wot");

		OAuth2 oauth = new OAuth2Builder().setStrict(false)
				.setAuthorizationUrl("https://api.worldoftanks.eu/wot/auth/login/")
				.setAuthParam("application_id", application_id).setRedirectUri(redirectUri).build();

		return oauth;
	}

	private OAuth2 buildDisqus(Properties properties, String redirectUri) {
		HttlSender sender = HttlSender.url("https://disqus.com").httpClient4().sender().build();

		String clientId = properties.getProperty("disqus.client_id");
		String clientSecret = properties.getProperty("disqus.client_secret");
		redirectUri = redirectUri.replace("{provider}", "disqus");

		OAuth2 oauth = new OAuth2Builder().setClientId(clientId).setClientSecret(clientSecret)
				.setTokenEndpoint(sender, "/api/oauth/2.0/access_token/").setAuthorizationUrl("/api/oauth/2.0/authorize/")
				.setRedirectUri(redirectUri).build();
		return oauth;
	}

	private OAuth2 buildGithub(Properties properties, String redirectUri) {
		HttlSender sender = HttlSender.url("https://github.com").httpClient4().sender()
				.addHeader("Accept", "application/json").build();

		String clientId = properties.getProperty("github.client_id");
		String clientSecret = properties.getProperty("github.client_secret");
		redirectUri = redirectUri.replace("{provider}", "github");

		OAuth2 oauth = new OAuth2Builder().setClientId(clientId).setClientSecret(clientSecret)
				.setTokenEndpoint(sender, "/login/oauth/access_token").setAuthorizationUrl("/login/oauth/authorize")
				.setRedirectUri(redirectUri).build();
		return oauth;
	}

	private OAuth2 buildGoogle(Properties properties, String redirectUri) {
		HttlSender sender = HttlSender.url("https://accounts.google.com").httpClient4().sender().build();

		String clientId = properties.getProperty("google.client_id");
		String clientSecret = properties.getProperty("google.client_secret");
		redirectUri = redirectUri.replace("{provider}", "google");

		OAuth2 oauth = new OAuth2Builder().setClientId(clientId).setClientSecret(clientSecret)
				.setTokenEndpoint(sender, "/o/oauth2/token").setAuthorizationUrl("/o/oauth2/auth").setRedirectUri(redirectUri)
				.build();
		return oauth;
	}

	private OAuth2 buildLinkedIn(Properties properties, String redirectUri) {
		HttlSender sender = HttlSender.url("https://www.linkedin.com").httpClient4().sender().build();

		String clientId = properties.getProperty("linkedin.client_id");
		String clientSecret = properties.getProperty("linkedin.client_secret");
		redirectUri = redirectUri.replace("{provider}", "linkedin");

		OAuth2 oauth = new OAuth2Builder().setClientId(clientId).setClientSecret(clientSecret)
				.setTokenEndpoint(sender, "/uas/oauth2/accessToken").setAuthorizationUrl("/uas/oauth2/authorization")
				.setRedirectUri(redirectUri).build();
		return oauth;
	}

	/**
	 * https://developers.facebook.com/docs/facebook-login/manually-build-a-login-flow/v2.1
	 */
	private OAuth2 buildFacebook(Properties properties, String redirectUri) {
		HttlSender sender = HttlSender.url("https://graph.facebook.com").httpClient4().sender().build();

		String clientId = properties.getProperty("facebook.client_id");
		String clientSecret = properties.getProperty("facebook.client_secret");
		redirectUri = redirectUri.replace("{provider}", "facebook");

		OAuth2 oauth = new OAuth2Builder().setClientId(clientId).setClientSecret(clientSecret)
				.setAuthParam("display", "popup").setTokenEndpoint(sender, "/oauth/access_token")
				.setAuthorizationUrl("https://www.facebook.com/dialog/oauth").setRedirectUri(redirectUri).build();
		return oauth;
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
