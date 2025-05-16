package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RobiSmsResponse {
	private String status;
	private String description;
	private String msgCost;
	private String currentBalance;
	private String contentType;
	private String msgCount;
	private String errorCode;
	private String messageId;
}
