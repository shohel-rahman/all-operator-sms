package com.desco.sms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import org.springframework.stereotype.Service;

import com.desco.sms.model.SmsModel;
import com.desco.sms.projection.MobileOperatorCode;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class SmsService {

	private List<SmsModel> getTeleSms() {
		List<SmsModel> pulledSmsList = new ArrayList<SmsModel>();
		try {
			pulledSmsList = teleRepository.findTeletalkSms(longCodes, shortCodes, maxChunkSize * 10);
			log.info("SMS pulling successfull.");
			return pulledSmsList;
		} catch (Exception e) {
			log.warn("Exception while getting SMS from DB " + e.getMessage());
			return pulledSmsList;
		}
	}
	
	public int countPendingSms() {
		return teleRepository.pendingSmsCount(longCodes, shortCodes);
	}
	
	private int updateSmsStatus(List<SmsModel> smsList, Queue<String> individualSentStatus,
			Queue<String> individualSentIdentifier) {

		for (SmsModel sms : smsList) {
			sms.setSentDate(LocalDateTime.now());
			sms.setSentStatus("Y");
			if (individualSentStatus.peek().equals("RS")) {
				sms.setOperatorId(individualSentIdentifier.poll());
			}
			sms.setHandsetDelivery(individualSentStatus.poll());
		}

		try {
			teleRepository.saveAll(smsList);
			log.info("Updated sent_status, sent_date, hadset_delivery, operator_id for each extracted SMS");
		} catch (Exception e) {
			log.warn("Exception while updating each extracted SMS " + e.getMessage());
		}

		return smsList.size();
	}
	
	private int updatePullStatus(List<Integer> smsIdList) {
		try {
			return teleRepository.updateSentStatus(smsIdList);
		} catch (Exception e) {
			log.warn("Exception at sent/pull status update " + e.getMessage());
			return 0;
		}
	}
	
}
