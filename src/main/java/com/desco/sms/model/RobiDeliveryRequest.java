package com.desco.sms.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RobiDeliveryRequest {
	private String sender;
	private String messageId;
	private String[] receiver;
	private int smsQId;
}
