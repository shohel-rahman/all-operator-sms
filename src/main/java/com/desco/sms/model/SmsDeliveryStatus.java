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
@Table(name = "SMS_DELIVERY_STATUS" , schema = "SHOHEL")
public class SmsDeliveryStatus {
	@Id
	@Column(name = "SMS_ID")
	private Integer smsId;
	
	@Column(name = "MOBILE_NO")
	private String mobileNo;
	
	@Column(name = "STATUS_CODE")
	private String statusCode;
	
	@Column(name = "STATUS_DESCRIPTION")
	private String statusDescription;
	
	@Column(name = "DELIVERY_STATUS")
	private String deliveryStatus;
	
	@Column(name = "CREATE_DATE")
	private LocalDateTime createDate;
	
}
