package net.anthavio.vinbudin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpSession;

import net.anthavio.httl.HttlResponse;
import net.anthavio.httl.HttlResponseExtractor;
import net.anthavio.httl.HttlResponseExtractor.ExtractedResponse;
import net.anthavio.httl.HttlSender;
import net.anthavio.httl.auth.OAuth2;
import net.anthavio.httl.auth.OAuth2Builder;
import net.anthavio.httl.auth.OAuthTokenResponse;
import net.anthavio.httl.util.HttlUtil;
import net.anthavio.vinbudin.vui.ChatUI;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OAuthController {

	public static enum OAuthProvider {
		GOOGLE, FACEBOOK, GITHUB, LINKEDIN, DISQUS;//, BITLY, WOT;

		private OAuth2 oauth;

		private String scopes;

		public static OAuthProvider getByName(String provider) {
			provider = provider.toLowerCase();
			OAuthProvider[] values = values();
			for (OAuthProvider value : values) {
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

	public OAuthController() {
		Properties properties = load("oauth.properties");
		String redirectUri = properties.getProperty("oauth.redirect_uri");
		OAuthProvider.GOOGLE.setOAuth(buildGoogle(properties, redirectUri), "openid profile");
		OAuthProvider.FACEBOOK.setOAuth(buildFacebook(properties, redirectUri), "public_profile");
		OAuthProvider.LINKEDIN.setOAuth(buildLinkedIn(properties, redirectUri), "r_basicprofile");
		OAuthProvider.GITHUB.setOAuth(buildGithub(properties, redirectUri), "");
		OAuthProvider.DISQUS.setOAuth(buildDisqus(properties, redirectUri), "read");
		//OAuthProvider.BITLY.setOAuth(buildBitly(properties, redirectUri), "");
		//OAuthProvider.WOT.setOAuth(buildWot(properties, redirectUri), "");
	}

	@RequestMapping("authorize/{provider}")
	public String authorize(@PathVariable(value = "provider") String provider) {
		OAuthProvider op = OAuthProvider.getByName(provider);
		String url = op.getOAuth().getAuthorizationUrl(op.getScopes(), String.valueOf(System.currentTimeMillis()));
		return "redirect:" + url;
	}

	@ResponseBody
	@RequestMapping(value = "callback/wot", params = "status=error")
	public String wotErrorCallback(@RequestParam(value = "status") String status,
			@RequestParam(value = "message") String message, @RequestParam(value = "code") String code) {
		if ("AUTH_CANCEL".equals(message)) {
			return oauthErrorCallback("wot", "access_denied", null);
		} else {
			return oauthErrorCallback("wot", code, message);
		}
	}

	@ResponseBody
	@RequestMapping(value = "callback/wot", params = "status=ok")
	public String wotSuccessCallback(@RequestParam(value = "status") String status,
			@RequestParam(value = "access_token") String access_token, @RequestParam(value = "nickname") String nickname,
			@RequestParam(value = "account_id") String account_id, @RequestParam(value = "expires_at") String expires_at) {
		return "Hello " + nickname;
	}

	/**
	 * When user denies or on any error Bitly does not bother with returning anything...
	 */
	@ResponseBody
	@RequestMapping(value = "callback/bitly")
	public String bitlyErrorCallback() {
		return oauthErrorCallback("bitly", "access_denied", "bitly_says_no");
	}

	@ResponseBody
	@RequestMapping(value = "callback/bitly", params = "code")
	public String bitlyCodeCallback(@RequestParam(value = "code") String code) {
		OAuthProvider op = OAuthProvider.getByName("bitly");
		Map<String, String> response = op.getOAuth().access(code)
				.get(new XFormEncodedExtractor("application/x-www-form-urlencoded"));
		String access_token = response.get("access_token");
		response.get("login");
		return access_token;
	}

	@ResponseBody
	@RequestMapping(value = "callback/{provider}", params = "error")
	public String oauthErrorCallback(@PathVariable(value = "provider") String provider,
			@RequestParam(value = "error") String error,
			@RequestParam(value = "error_description", required = false) String error_description) {
		if (error_description != null) {
			return error + ": " + error_description;
		} else {
			return error;
		}
		//When user clicks Deny -> error=access_denied
		//return service.oauthErrorCallback(error);
	}

	@RequestMapping(value = "callback/{provider}", params = "code")
	public String oauthCodeCallback(@PathVariable(value = "provider") String provider,
			@RequestParam(value = "code") String code, HttpSession session) {

		OAuthProvider p = OAuthProvider.getByName(provider);
		OAuthTokenResponse tokenResponse;

		String name;
		switch (p) {
		case FACEBOOK:
			name = facebook(code);
			break;
		case GOOGLE:
			tokenResponse = p.getOAuth().access(code).get();
			name = google(tokenResponse.getAccess_token());
			break;
		case GITHUB:
			tokenResponse = p.getOAuth().access(code).get();
			name = github(tokenResponse.getAccess_token());
			break;
		case LINKEDIN:
			tokenResponse = p.getOAuth().access(code).get();
			name = linkedin(tokenResponse.getAccess_token());
			break;
		case DISQUS:
			Map mapOfToken = p.getOAuth().access(code).get(Map.class);
			name = (String) mapOfToken.get("username");
			break;
		default:
			throw new IllegalStateException("Unknown " + p);
		}

		session.setAttribute(ChatUI.ME_KEY, new ChatMan(name, p));
		return "redirect:/vui";
	}

	private String facebook(String code) {
		//https://developers.facebook.com/docs/graph-api/reference/v2.1/user

		Map<String, String> mapOfToken = OAuthProvider.FACEBOOK.getOAuth().access(code)
				.get(new XFormEncodedExtractor("text/plain"));
		String access_token = mapOfToken.get("access_token");

		HttlSender sender = HttlSender.url("https://graph.facebook.com").build();
		ExtractedResponse<Map> response = sender.GET("/me").header("Authorization", "Bearer " + access_token)
				.extract(Map.class);
		Map map = response.getBody();
		return (String) map.get("name");
	}

	private String linkedin(String access_token) {
		//https://developer.linkedin.com/documents/profile-api
		HttlSender sender = HttlSender.url("https://api.linkedin.com").build();
		ExtractedResponse<Map> response = sender.GET("/v1/people/~").header("Authorization", "Bearer " + access_token)
				.header("x-li-format", "json").extract(Map.class);
		Map map = response.getBody();
		//System.out.println(map);
		return (String) map.get("firstName") + " " + map.get("lastName");

	}

	private String google(String access_token) {
		HttlSender sender = HttlSender.url("https://www.googleapis.com").build();
		ExtractedResponse<Map> response = sender.GET("/plus/v1/people/me")
				.header("Authorization", "Bearer " + access_token).extract(Map.class);
		Map map = response.getBody();
		return (String) map.get("displayName");
	}

	private String github(String access_token) {
		HttlSender sender = HttlSender.url("https://api.github.com").build();
		ExtractedResponse<Map> response = sender.GET("/user").header("Authorization", "token " + access_token)
				.extract(Map.class);
		Map map = response.getBody();
		return (String) map.get("login");
	}

	static class XFormEncodedExtractor implements HttlResponseExtractor<Map<String, String>> {

		private final String mediaType;

		public XFormEncodedExtractor(String mediaType) {
			this.mediaType = mediaType;
		}

		@Override
		public Map<String, String> extract(HttlResponse response) throws IOException {
			int code = response.getHttpStatusCode();
			if (code < 200 || code > 299) {
				throw new IllegalArgumentException("Unexpected status code " + response);
			}
			if (!mediaType.equals(response.getMediaType())) {
				throw new IllegalArgumentException("Unexpected media type " + response);
			}
			Map<String, String> map = new HashMap<String, String>();
			String line = HttlUtil.readAsString(response);
			String[] pairs = line.split("\\&");
			for (int i = 0; i < pairs.length; i++) {
				String[] fields = pairs[i].split("=");
				String name = URLDecoder.decode(fields[0], response.getEncoding());
				String value = URLDecoder.decode(fields[1], response.getEncoding());
				map.put(name, value);
			}
			return map;
		}

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

	private OAuth2 buildBitly(Properties properties, String redirectUri) {
		HttlSender sender = HttlSender.url("https://api-ssl.bitly.com").httpClient4().sender().build();

		String clientId = properties.getProperty("bitly.client_id");
		String clientSecret = properties.getProperty("bitly.client_secret");
		redirectUri = redirectUri.replace("{provider}", "bitly");

		OAuth2 oauth = new OAuth2Builder().setClientId(clientId).setClientSecret(clientSecret)
				.setTokenEndpoint(sender, "/oauth/access_token").setAuthorizationUrl("https://bitly.com/oauth/authorize")
				.setRedirectUri(redirectUri).build();
		return oauth;
	}

	private OAuth2 buildWot(Properties properties, String redirectUri) {

		String application_id = properties.getProperty("wot.application_id");
		redirectUri = redirectUri.replace("{provider}", "wot");

		OAuth2 oauth = new OAuth2Builder().setStrict(false)
				.setAuthorizationUrl("https://api.worldoftanks.eu/wot/auth/login/")
				.setAuthParam("application_id", application_id).setRedirectUri(redirectUri).build();

		return oauth;
	}

	private Properties load(String name) {
		String property = System.getProperty(name, name);
		try {
			InputStream stream;
			File file = new File(property);
			if (file.exists()) {
				stream = new FileInputStream(file);
			} else {
				ClassLoader loader = Thread.currentThread().getContextClassLoader();
				stream = loader.getResourceAsStream(property);
				if (stream == null) {
					throw new IllegalArgumentException("Properties resource not found: " + property);
				}
			}

			Properties properties = new Properties();
			properties.load(stream);
			return properties;
		} catch (IOException iox) {
			throw new IllegalStateException("Properties resource " + property + " failed to load", iox);
		}
	}
}
