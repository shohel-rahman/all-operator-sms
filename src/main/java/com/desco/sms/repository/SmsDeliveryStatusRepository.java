package com.desco.sms.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.desco.sms.model.SmsDeliveryStatus;

public interface SmsDeliveryStatusRepository extends JpaRepository<SmsDeliveryStatus, Integer>{

}
