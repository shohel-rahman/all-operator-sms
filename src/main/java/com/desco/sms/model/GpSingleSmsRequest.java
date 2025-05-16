package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
public class GpSingleSmsRequest {

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
	
	public void setFixedFields(String gpUser, String gpPassword, String sender) {
		setUsername(gpUser);
		setPassword(gpPassword);
		setApicode("1");
		setCountrycode("880");
		setCli(sender);
		setMessagetype("1");
		setBill_msisdn("01708403636");
		setTran_type("T");
		setRequest_type("S");
		setRn_code("71");
	}

	public void setClienttransid(String id) {
		this.clienttransid = id + "-" + System.currentTimeMillis();
		int padding = 25 - this.clienttransid.length();
		for(int i=0;i<padding; i++) {
			this.clienttransid += "D";
		}
		
	}
	
}
