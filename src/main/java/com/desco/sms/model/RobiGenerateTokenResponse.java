package com.desco.sms.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Log4j2
public class RobiGenerateTokenResponse {
	private String token;
	private String refresh_token;
//	private long creationTime;	
	
	public String formatRefreshTokenRequest() {
		String jsonRequestBody = "";
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			jsonRequestBody = objectMapper.writeValueAsString(this);
			log.info("Robi refresh token request JSON is generated");
		} catch (JsonProcessingException e) {
			log.warn("Exception while converting Robi refersh token request object to JSON : " + e.getMessage());
		}
		return jsonRequestBody;
	}
}
