package com.desco.sms.model;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Data
@Entity
//@Table(name = "SMS_QUEUE_TBL_TEST" , schema = "SMS_STORAGE")
@Table(name = "SMS_QUEUE_TBL_TEST" , schema = "SHOHEL")
public class SmsModel {

	@Id
	@Column(name = "ID")
	private Integer id;

	@Column(name = "MOBILE_NO")
	private String mobileNo;

	@Column(name = "SMS_TEXT")
	private String smsText;

	@Column(name = "SENT_DATE")
	private LocalDateTime sentDate;

	@Column(name = "SENT_STATUS")
	private String sentStatus;

	@Column(name = "OPERATOR_ID")
	private String operatorId;

	@Column(name = "HANDSET_DELIVERY")
	private String handsetDelivery;
	
	@Column(name = "SENDER_EID")
	private String senderId;
	
	@Column(name = "TRACKING_NUMBER")
	private String trackingNumber;
}
