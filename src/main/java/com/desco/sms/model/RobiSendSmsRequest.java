package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RobiSendSmsRequest {
	private String sender;
	private String msgType;
	private String requestType;
	private String contentType;
	private String[] receiver;
	private String content;
	
	public void setFixedFields(String smsSender) {
		setSender(smsSender);
		setMsgType("T");
		setRequestType("S");
		setContentType("1");
	}

}
