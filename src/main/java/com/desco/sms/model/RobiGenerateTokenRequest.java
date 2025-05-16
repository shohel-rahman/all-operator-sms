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
public class RobiGenerateTokenRequest {
	
	private String username;	
	private String password;
	
	public String formatTokenGenerateRequest() {
		String jsonRequestBody = "";
		ObjectMapper objectMapper = new ObjectMapper();
		try {
			jsonRequestBody = objectMapper.writeValueAsString(this);
			log.info("Robi token generate request JSON is generated");
		} catch (JsonProcessingException e) {
			log.warn("Exception while converting Robi token generate request object to JSON : " + e.getMessage());
		}
		return jsonRequestBody;
	}
}
