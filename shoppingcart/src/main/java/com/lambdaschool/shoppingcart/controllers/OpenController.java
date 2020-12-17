package com.lambdaschool.shoppingcart.controllers;


import com.lambdaschool.shoppingcart.models.User;
import com.lambdaschool.shoppingcart.models.UserMinimum;
import com.lambdaschool.shoppingcart.models.UserRoles;
import com.lambdaschool.shoppingcart.services.RoleService;
import com.lambdaschool.shoppingcart.services.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.TokenStore;
import org.springframework.stereotype.Controller;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@RestController
public class OpenController {
	@Autowired
	private UserService userService;
	@Autowired
	private RoleService roleService;
	@Autowired
	private TokenStore tokenStore;

	@PostMapping(value = "/createnewuser",
			consumes = {"application/json"},
			produces = {"application/json"})
	public ResponseEntity<?> addSelf(
			HttpServletRequest httpServletRequest,
			@Valid
			@RequestBody
					UserMinimum newminuser)
	throws URISyntaxException
	{
		// Create the user
		User newuser = new User();

		newuser.setUsername(newminuser.getUsername());
		newuser.setPassword(newminuser.getPassword());
		newuser.setPrimaryemail(newminuser.getPrimaryemail());


		// add the default role of user
		Set<UserRoles> newRoles = new HashSet<>();
		newRoles.add(new UserRoles(newuser,
				roleService.findByName("USER")));
		newuser.setRoles(newRoles);

		newuser = userService.save(newuser);

		// set the location header for the newly created resource
		// The location comes from a different controller!
		HttpHeaders responseHeaders = new HttpHeaders();
		URI newUserURI = ServletUriComponentsBuilder.fromUriString(httpServletRequest.getServerName() + ":" + httpServletRequest.getLocalPort() + "/users/user/{userId}")
				.buildAndExpand(newuser.getUserid())
				.toUri();
		responseHeaders.setLocation(newUserURI);

		// return the access token
		// To get the access token, surf to the endpoint /login (which is always on the server where this is running)
		// just as if a client had done this.
		RestTemplate restTemplate = new RestTemplate();
		String       requestURI   = "http://localhost" + ":" + httpServletRequest.getLocalPort() + "/login";

		List<MediaType> acceptableMediaTypes = new ArrayList<>();
		acceptableMediaTypes.add(MediaType.APPLICATION_JSON);

		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
		headers.setAccept(acceptableMediaTypes);
		headers.setBasicAuth(System.getenv("OAUTHCLIENTID"),
				System.getenv("OAUTHCLIENTSECRET"));

		MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
		map.add("grant_type",
				"password");
		map.add("scope",
				"read write trust");
		map.add("username",
				newminuser.getUsername());
		map.add("password",
				newminuser.getPassword());

		HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map,
				headers);

		String theToken = restTemplate.postForObject(requestURI,
				request,
				String.class);

		return new ResponseEntity<>(theToken,
				responseHeaders,
				HttpStatus.CREATED);
	}

	/**
	 * Removes the token for the signed on user. The signed user will lose access to the application. They would have to sign on again.
	 *
	 * <br>Example: <a href="http://localhost:2019/logout">http://localhost:2019/logout</a>
	 *
	 * @param request the Http request from which we find the authorization header which includes the token to be removed
	 */
	// yes, both endpoints are mapped to the same Java method! So, either one will work.
//	@GetMapping(value = {"/oauth/revoke-token", "/logout"},
//			produces = "application/json")
//	public ResponseEntity<?> logoutSelf(HttpServletRequest request)
//	{
//		String authHeader = request.getHeader("Authorization");
//		if (authHeader != null)
//		{
//			// find the token
//			String tokenValue = authHeader.replace("Bearer",
//					"")
//					.trim();
//			// and remove it!
//			OAuth2AccessToken accessToken = tokenStore.readAccessToken(tokenValue);
//			tokenStore.removeAccessToken(accessToken);
//		}
//
//		return new ResponseEntity<>(HttpStatus.OK);
//	}


}
