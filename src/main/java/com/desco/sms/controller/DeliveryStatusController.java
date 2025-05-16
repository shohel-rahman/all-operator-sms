package com.desco.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desco.sms.service.BlDeliveryService;
import com.desco.sms.service.GpDeliveryService;
import com.desco.sms.service.RobiDeliveryService;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@EnableScheduling
@RequestMapping("/sms/delivery/api")
public class DeliveryStatusController {
	@Autowired
	GpDeliveryService gpDelivery;
	@Autowired
	RobiDeliveryService robiDelivery;
	@Autowired
	BlDeliveryService banglaDelivery;
	

	@Scheduled(cron = "${banglalink.dlr.schedule}")	
	@PostMapping("/banglalink")
	public void triggerBanglalink() {
		banglaDelivery.initiateDeliveryChecking();
	}
	
//	@Scheduled(cron = "*/5 * 7-23 * * *")
//	@Scheduled(cron = "${gp.dlr.schedule}")	
	@PostMapping("/gp")
	public void triggerGp() {
		gpDelivery.initiateDeliveryChecking();

	}

//	@Scheduled(cron = "*/15 * 7-23 * * *")
//	@Scheduled(cron = "${robi.dlr.schedule}")	
	@PostMapping("/robi")
	public void triggerRobi() {
			robiDelivery.initiateDeliveryChecking();

	}

}
