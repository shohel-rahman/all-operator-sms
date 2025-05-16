package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class BlDeliveryRequest {
	private String username;
	private String password;	
	private String apicode;
	private String[] msisdn;
	private String countrycode;
	private String cli;
	private String messagetype;
	private String clienttransid;	
	private String operatortransid;
	
	public void setFixedFields(String blUser, String blPassword, String blSender) {
		setUsername(blUser);
		setPassword(blPassword);
		setApicode("4");
		setCountrycode("880");
		setCli(blSender);
		setMessagetype("1");
	}

	public void setClienttransid(String id) {
		this.clienttransid = id + System.currentTimeMillis();	
	}
}
