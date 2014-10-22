package net.anthavio.vinbudin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class OAuthController {

	@Autowired
	OAuthService service;

	@RequestMapping("oauth/callback/facebook")
	public void callback() {
		System.out.println("xxxxxxxxxxxxxxxxxx");
	}

	@RequestMapping(value = "callback/facebook", params = "error")
	public String facebookErrorCallback(@RequestParam(value = "error") String error,
			@RequestParam(value = "error_description", required = false) String error_description) {
		return null;
		//When user clicks Deny -> error=access_denied
		//return service.oauthErrorCallback(error);
	}

	@RequestMapping(value = "callback/facebook", params = "code")
	public String facebookCodeCallback(@RequestParam(value = "code") String code) {
		return "";
		//return service.oauthCodeCallback(code);
	}
}
