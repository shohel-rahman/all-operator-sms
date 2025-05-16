package com.desco.sms.projection;

import lombok.Data;

@Data
public class TeleSendSmsRequest {
	private String op;
	private String chunk;
	private String user;
	private String pass;
	private String serverName;
	private String smsClass;
	private String sms;
	private String mobile;
	private String charset;
	private String validity;
	private String a_key;
	private String p_key;
	private String cid;

	public void setCharset(String charset, int repeatitions) {
		if(repeatitions <= 1) {
			this.charset = charset;
		}
		else {
			this.charset = charset;
			for(int i= 1; i<repeatitions; i++) {
				this.charset += "|" + charset;
			}
		}
	}
}
