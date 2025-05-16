package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class GpDeliveryRequest {
	private  String username;
	private  String password;	
	private String apicode;
	private String[] msisdn;
	private String countrycode;
	private String cli;
	private String messagetype;
	private String clienttransid;	
	private String operatortransid;
	
	public void setFixedFields(String gpUser, String gpPassword, String gpSender) {
		setUsername(gpUser);
		setPassword(gpPassword);
		setApicode("4");
		setCountrycode("880");
		setCli(gpSender);
		setMessagetype("1");
	}

	public void setClienttransid(String id) {
		this.clienttransid = id + "-" + System.currentTimeMillis();
		int padding = 25 - this.clienttransid.length();
		for(int i=0;i<padding; i++) {
			this.clienttransid += "D";
		}		
	}
}
