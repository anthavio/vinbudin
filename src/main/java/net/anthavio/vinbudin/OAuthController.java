package net.anthavio.vinbudin;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.Map;

import net.anthavio.httl.HttlResponse;
import net.anthavio.httl.HttlResponseExtractor;
import net.anthavio.httl.auth.OAuthTokenResponse;
import net.anthavio.httl.util.HttlUtil;
import net.anthavio.vinbudin.OAuthService.OAuthProviders;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OAuthController {

	@Autowired
	OAuthService service;

	@RequestMapping("authorize/{provider}")
	public String authorize(@PathVariable(value = "provider") String provider) {
		OAuthProviders op = OAuthProviders.getByName(provider);
		//System.out.println(op.getOAuth().);
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
		OAuthProviders op = OAuthProviders.getByName("bitly");
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

	@ResponseBody
	@RequestMapping(value = "callback/{provider}", params = "code")
	public String oauthCodeCallback(@PathVariable(value = "provider") String provider,
			@RequestParam(value = "code") String code) {

		OAuthProviders op = OAuthProviders.getByName(provider);
		OAuthTokenResponse tokenResponse;
		if (op == OAuthProviders.FACEBOOK) {
			Map<String, String> map = op.getOAuth().access(code).get(new XFormEncodedExtractor("text/plain"));
			String access_token = map.get("access_token");
			String expires = map.get("expires");
			tokenResponse = new OAuthTokenResponse(access_token);
		} else {
			tokenResponse = op.getOAuth().access(code).get();
			System.out.println(tokenResponse);
		}

		return "" + tokenResponse;
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
}
