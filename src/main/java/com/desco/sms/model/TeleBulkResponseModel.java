package com.desco.sms.model;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Data
@JsonIgnoreProperties({"Server_Time","server", "sms_class", "current_credit_master"})
//@Table(name = "TELE_BULK_RESPONSE", schema = "SMS_SENDER")
@Table(name = "BULK_RESPONSE", schema = "SHOHEL")
public class TeleBulkResponseModel {

	@Id
	@Column (name = "CID")
	String cid;
	String chunk_id;
	String status;
	String scode;
	String details;
	String processing_details;
	String credit_deducted;
	String current_credit;
	String credit_inheritance;
	LocalDateTime create_date;
}
