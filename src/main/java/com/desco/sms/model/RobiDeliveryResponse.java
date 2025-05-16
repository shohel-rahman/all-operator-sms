package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RobiDeliveryResponse {

	private String status;
	private String description;
	private String msgCost;
	private String duringMsgBalance;
	private String contentType;
	private String msgCount;
	private String errorCode;
	private String messageId;
}
