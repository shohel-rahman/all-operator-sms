package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class BanglalinkSmsRequest {

	private String username;
	private String password;	
	private String apicode;
	private String[] msisdn;
	private String countrycode;
	private String cli;
	private String messagetype;
	private String message;
	private String clienttransid;
	private String bill_msisdn;
	private String tran_type;
	private String request_type;
	private String rn_code;
	
	public void setFixedFields(String blUser, String blPassword, String blSender) {
		setUsername(blUser);
		setPassword(blPassword);
		setApicode("5");
		setCountrycode("880");
		setCli(blSender);
		setMessagetype("1");
		setBill_msisdn("01969931063");
		setTran_type("T");
		setRequest_type("S");
		setRn_code("91");
	}

	public void setClienttransid(String id) {
		this.clienttransid = id + System.currentTimeMillis();		
	}
	
}
