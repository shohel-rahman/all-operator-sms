package com.desco.sms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.desco.sms.model.SmsDeliveryStatus;
import com.desco.sms.model.RobiDeliveryRequest;
import com.desco.sms.model.RobiDeliveryResponse;
import com.desco.sms.model.SmsModel;
import com.desco.sms.projection.MobileOperatorCode;
import com.desco.sms.repository.SmsDeliveryStatusRepository;
import com.desco.sms.repository.SmsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class RobiDeliveryService {
	@Value("${robi.sender}")
	String smsSender;
	@Value("${robi.url}")
	String baseUrl;
	@Value("${robi.max.chunk.size}")
	int maxChunkSize;
	
	@Autowired
	SmsRepository smsRepository;
	@Autowired
	RobiTokenService robiToken = new RobiTokenService();
	@Autowired
	SmsDeliveryStatusRepository deliveryStatusRepo;
	
	static final String primaryOperator = MobileOperatorCode.ROBI.toString();
	private boolean busy = false;
	
	List<SmsModel> robiSmsForDeliveryCheck = new ArrayList<SmsModel>();		
	
	public void initiateDeliveryChecking() {
		int pendingDlr = smsRepository.pendingDlrCount("RB");
		long startTime = 0;
		if(!busy && pendingDlr > 0) {
			log.info("(" + primaryOperator + ") " + pendingDlr + " Delivery check pending. New cycle can be started");
			startTime = System.currentTimeMillis();
			checkRobiDelivery();
			log.info("(" + primaryOperator + ") "+ (System.currentTimeMillis() - startTime) + " ms elapsed for completing delivery check cycle");
		}
		else if (busy && pendingDlr > 0) {
			log.info("(" + primaryOperator + ") " + pendingDlr
					+ " Delivery check pending. Application busy new cycle can not be started");
		} else {
			log.info("(" + primaryOperator + ") " + pendingDlr + " Delivery check pending.");
		}
	}
	
	public void checkRobiDelivery() {
		busy =true;
		long startTime = System.currentTimeMillis();
		
		robiSmsForDeliveryCheck =  smsRepository.findSentSms("RB", maxChunkSize);
		log.info("(" + primaryOperator + ") Pulled " + robiSmsForDeliveryCheck.size() + " SMS for delivery checking in "+ (System.currentTimeMillis() - startTime) + " ms");
		
		updateDeliveryCheckStatus("DLR CHECKING");
		
		List<RobiDeliveryRequest> queryParamsForRequest = checkDeliveryRequestFormatter(robiSmsForDeliveryCheck);
			
		callDeliveryApi(queryParamsForRequest);
		
		robiSmsForDeliveryCheck.clear();	
		busy=false;		
	}
	
	private void callDeliveryApi(List<RobiDeliveryRequest> queryParams) {
		
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		String deliveryUrl = baseUrl + "/sms/status";
		headers.setBearerAuth(robiToken.serveToken());
		
		long startTime = 0;
		
		ObjectMapper deliveryStatusMapper = new ObjectMapper();
		RobiDeliveryResponse smsDeliveryResponse = new RobiDeliveryResponse();
		
		List<SmsDeliveryStatus> saveSmsDelivery = new ArrayList<SmsDeliveryStatus>();

		startTime = System.currentTimeMillis();
		ResponseEntity<String> checkDeliveryResponse = new ResponseEntity<>(HttpStatus.NO_CONTENT);
		
		String singleQuery = "";
		for (int i = 0; i < queryParams.size(); i++) {

			HttpEntity<String> httpRequest = new HttpEntity<>(headers);
			singleQuery = UriComponentsBuilder.fromUriString(deliveryUrl)
	        .queryParam("sender", queryParams.get(i).getSender())
	        .queryParam("messageId", queryParams.get(i).getMessageId())
	        .queryParam("receiver", (Object[]) queryParams.get(i).getReceiver()).toUriString();
			
			SmsDeliveryStatus singleStatus = new SmsDeliveryStatus();
			
			try {				
				checkDeliveryResponse = restTemplate.exchange(singleQuery, HttpMethod.GET, httpRequest, String.class);				
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") " + "Exception while calling check SMS delivery API with request URL: "
						+ singleQuery + "  " + e.getMessage());
			}
			
			try {
			smsDeliveryResponse = deliveryStatusMapper.readValue(checkDeliveryResponse.getBody(), RobiDeliveryResponse.class);
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") "
						+ "Exception while decoding send SMS API response with response body: " + checkDeliveryResponse.getBody()
						+ "  " + e.getMessage());
			}	

			singleStatus.setCreateDate(LocalDateTime.now());			
			
			singleStatus.setStatusCode(smsDeliveryResponse.getErrorCode());
			singleStatus.setStatusDescription(smsDeliveryResponse.getStatus());
			singleStatus.setDeliveryStatus(smsDeliveryResponse.getDescription());
			
			singleStatus.setMobileNo(queryParams.get(i).getReceiver()[0].substring(2));
			singleStatus.setSmsId(queryParams.get(i).getSmsQId());
									
			saveSmsDelivery.add(singleStatus);
			
			if(smsDeliveryResponse.getErrorCode().equals("0")) {
				robiSmsForDeliveryCheck.get(i).setHandsetDelivery(smsDeliveryResponse.getDescription());
			}
			else {
				robiSmsForDeliveryCheck.get(i).setHandsetDelivery("ErrorCode:" + smsDeliveryResponse.getErrorCode());
			}			
		}		
		log.info("(" + primaryOperator + ") " + queryParams.size() + " Delivery calling completed in " + (System.currentTimeMillis() - startTime) + " ms");
		
		try {
			startTime = System.currentTimeMillis();
			deliveryStatusRepo.saveAll(saveSmsDelivery);
			log.info("(" + primaryOperator + ") " + "Inserted "+saveSmsDelivery.size() +" rows in SMS Delivery Status table in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while inserting data in SMS Delivery Status table " + e.getMessage());
		}
		
		try {
			startTime = System.currentTimeMillis();
			smsRepository.saveAll(robiSmsForDeliveryCheck);
			log.info("(" + primaryOperator + ") " + "Updated " + robiSmsForDeliveryCheck.size()+ " delivery status in SMS Queue table in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating delivery status in SMS Queue table " + e.getMessage());
		}
		
	}
	
	
	private List<RobiDeliveryRequest> checkDeliveryRequestFormatter (List<SmsModel> smsSentByRobi){
		
		List<RobiDeliveryRequest> queryParam = new ArrayList<RobiDeliveryRequest>();				
		String[] numberForRobi = new String[1];
		
		RobiDeliveryRequest rawDeliveryRequest = new RobiDeliveryRequest();
		for (SmsModel sms : smsSentByRobi) {
			rawDeliveryRequest.setSender(smsSender);
			rawDeliveryRequest.setMessageId(sms.getOperatorId());
			rawDeliveryRequest.setSmsQId(sms.getId());
			
			if (sms.getMobileNo().length() == 11) {
				numberForRobi[0] = "88" + sms.getMobileNo();
			} else numberForRobi[0] = sms.getMobileNo();
			rawDeliveryRequest.setReceiver(numberForRobi);
						
			queryParam.add(rawDeliveryRequest);
		}
		return queryParam;
	}

	
	private void updateDeliveryCheckStatus (String status) {
		for (SmsModel deliverySms : robiSmsForDeliveryCheck) {
			deliverySms.setHandsetDelivery(status);
		}
		try {
			long startTime = System.currentTimeMillis();
			smsRepository.saveAll(robiSmsForDeliveryCheck);
			log.info("(" + primaryOperator + ") " + "Updated sms delivery checking status of those SMS in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating sms delivery checking status those SMS " + e.getMessage());
		}
	}
	
	
}
