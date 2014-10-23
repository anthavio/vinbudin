package net.anthavio.vinbudin;

import net.anthavio.httl.auth.OAuthTokenResponse;
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
			return errorCallback("wot", "access_denied", null);
		} else {
			return errorCallback("wot", code, message);
		}
	}

	@ResponseBody
	@RequestMapping(value = "callback/wot", params = "status=ok")
	public String wotSuccessCallback(@RequestParam(value = "status") String status,
			@RequestParam(value = "access_token") String access_token, @RequestParam(value = "nickname") String nickname,
			@RequestParam(value = "account_id") String account_id, @RequestParam(value = "expires_at") String expires_at) {
		return "Hello " + nickname;
	}

	@ResponseBody
	@RequestMapping(value = "callback/{provider}", params = "error")
	public String errorCallback(@PathVariable(value = "provider") String provider,
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
	public String codeCallback(@PathVariable(value = "provider") String provider,
			@RequestParam(value = "code") String code) {

		OAuthProviders op = OAuthProviders.getByName(provider);
		OAuthTokenResponse tokenResponse;
		if (op == OAuthProviders.FACEBOOK) {
			String facebookResponse = op.getOAuth().access(code).get(String.class);
			final String xtoken = "access_token=";
			final String xexpires = "&expires=";
			int tokenStartIdx = facebookResponse.indexOf(xtoken);
			int expiryStartIdx = facebookResponse.indexOf(xexpires);
			if (tokenStartIdx != 0 || expiryStartIdx == -1) {
				throw new IllegalStateException("Unpareseable Facebook Token response: " + facebookResponse);
			}
			String access_token = facebookResponse.substring(xtoken.length(), expiryStartIdx);
			String expiry = facebookResponse.substring(expiryStartIdx + xexpires.length());
			tokenResponse = new OAuthTokenResponse(access_token);
		} else {
			tokenResponse = op.getOAuth().access(code).get();
			System.out.println(tokenResponse);
		}

		return "" + tokenResponse;
	}
}
