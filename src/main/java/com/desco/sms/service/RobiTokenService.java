package com.desco.sms.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.desco.sms.model.RobiGenerateTokenRequest;
import com.desco.sms.model.RobiGenerateTokenResponse;
import com.desco.sms.projection.MobileOperatorCode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RobiTokenService {

	@Value("${robi.url}")
	String baseUrl;
	@Value("${robi.user.name}")
	String userName;
	@Value("${robi.user.password}")
	String userPassword;
	@Value("${robi.token.lifetime}")
	long tokenLifetime;
	
	static final String primaryOperator = MobileOperatorCode.ROBI.toString();	
	private RobiGenerateTokenRequest robiCredentials;
	private RobiGenerateTokenResponse robiAuthToken = new RobiGenerateTokenResponse();
	long tokenCreationTime = 0;
	
	public String serveToken() {
		if (tokenCreationTime == 0 || System.currentTimeMillis() - tokenCreationTime >= tokenLifetime) {
			robiCredentials = new RobiGenerateTokenRequest(userName, userPassword);
			String userCredentials = robiCredentials.formatTokenGenerateRequest();
			tokenCreationTime = System.currentTimeMillis();
			getRobiAuthToken(userCredentials);
		} else if (System.currentTimeMillis() - tokenCreationTime > (tokenLifetime - 600000)
				&& System.currentTimeMillis() - tokenCreationTime < tokenLifetime ) {
			refreshToken();
		}
		
		return robiAuthToken.getToken();
	}
	
	private void getRobiAuthToken(String credentials) {
		// Setting up the HTTP Request Header
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String tokenGenUrl = baseUrl + "/auth/tokens";

		// Post For Object Method
		HttpEntity<String> httpRequest = new HttpEntity<>(credentials, headers);

		String tokenGenerateResponseJson = "";
		try {
			log.info("(" + primaryOperator + ") " + "Calling token generation API");
			tokenGenerateResponseJson = restTemplate.postForObject(tokenGenUrl, httpRequest, String.class);			
			tokenCreationTime = System.currentTimeMillis();
			log.info("(" + primaryOperator + ") " + "Token generation API resoponse received");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while calling token generation API " + e.getMessage());
		}

		ObjectMapper tokenGenResponseMapper = new ObjectMapper();
		try {
			log.info("(" + primaryOperator + ") " + "Decoding token generate API response");
			robiAuthToken = tokenGenResponseMapper.readValue(tokenGenerateResponseJson,
					RobiGenerateTokenResponse.class);
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while decoding token generate API response "
					+ e.getMessage());
		}
	}
	
	private void refreshToken() {
		String tokenRefreshUrl = baseUrl + "/auth/token/refresh";
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		headers.setBearerAuth(robiAuthToken.getRefresh_token());
		HttpEntity<String> httpRequest = new HttpEntity<>(headers);

		String tokenRefreshResponseJson = "";
		try {
			log.info("(" + primaryOperator + ") " + "Calling token refersh API");
			tokenRefreshResponseJson = restTemplate.postForObject(tokenRefreshUrl, httpRequest, String.class);
			tokenCreationTime = System.currentTimeMillis();
			log.info("(" + primaryOperator + ") " + "token refresh API resoponse received");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while calling token refresh API " + e.getMessage());
		}

		ObjectMapper tokenGenResponseMapper = new ObjectMapper();
		try {
			log.info("(" + primaryOperator + ") " + "Decoding token generate API response");
			robiAuthToken = tokenGenResponseMapper.readValue(tokenRefreshResponseJson, RobiGenerateTokenResponse.class);
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while decoding token generate API response "
					+ e.getMessage());
		}
	}
}
