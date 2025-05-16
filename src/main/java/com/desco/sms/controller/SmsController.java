package com.desco.sms.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.desco.sms.service.BanglalinkService;
import com.desco.sms.service.GpService;
import com.desco.sms.service.RobiService;
import com.desco.sms.service.TeletalkService;

import lombok.extern.log4j.Log4j2;

@RestController
@Log4j2
@EnableScheduling
@RequestMapping("/sms/api")
public class SmsController {

	@Value("${teletalk.operator.active}")
	private boolean isTeletalkActive;
	@Value("${gp.operator.active}")
	private boolean isGpActive;
	@Value("${robi.operator.active}")
	private boolean isRobiActive;
	@Value("${banglalink.operator.active}")
	private boolean isBanglalinkActive;

	@Autowired
	TeletalkService teletalkService;
	@Autowired
	RobiService robiService;
	@Autowired
	GpService gpService;
	@Autowired
	BanglalinkService banglaService;
	
	
//	@Scheduled(cron = "*/2 * 7-22 * * *")
	@Scheduled(cron = "${teletalk.send.schedule}")	
	@PostMapping("/teletalk")
	public void triggerTeletalk() {
		if (isTeletalkActive) {
			teletalkService.operateTeletalk();
		}
	}

//	@Scheduled(cron = "*/3 * 7-22 * * *")
	@Scheduled(cron = "${gp.send.schedule}")	
	@PostMapping("/gp")
	public void triggerGp() {
		if (isGpActive) {
			gpService.operateGp();
		}
	}

//	@Scheduled(cron = "*/1 * * * * *")
	@Scheduled(cron = "${robi.send.schedule}")	
	@PostMapping("/robi")
	public void triggerRobi() {
		if (isRobiActive) {
			robiService.operateRobi();
		}
	}

//	@Scheduled(cron = "*/2 * * * * *")
	@Scheduled(cron = "${banglalink.send.schedule}")	
	@PostMapping("/banglalink")
	public void triggerBanglalink() {
		if (isBanglalinkActive) {
			banglaService.operateBanglalink();
		}
	}
}
