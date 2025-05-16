package com.desco.sms.projection;

import lombok.Data;

@Data
public class DeliverySmsModel {

	private Integer id;
	private String mobileNo;
	private String operatorId;
	private String sentStatus;
	private String senderId;
	private String trackingNumber;
	
	public DeliverySmsModel(Integer id, String mobileNo, String operatorId, String sentStatus, String senderId, String trackingNumber) {
		super();
		this.id = id;
		this.mobileNo = mobileNo;
		this.operatorId = operatorId;
		this.sentStatus = sentStatus;
		this.senderId = senderId;
		this.trackingNumber = trackingNumber;
	}	
}
