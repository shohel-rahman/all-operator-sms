package com.desco.sms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.desco.sms.model.BlDeliveryRequest;
import com.desco.sms.model.BlDeliveryResponse;
import com.desco.sms.model.SmsDeliveryStatus;
import com.desco.sms.model.SmsModel;
import com.desco.sms.projection.BlCheckDeliveryResponseField;
import com.desco.sms.projection.MobileOperatorCode;
import com.desco.sms.repository.SmsDeliveryStatusRepository;
import com.desco.sms.repository.SmsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class BlDeliveryService {

	@Value("${banglalink.sender}")
	String smsSender;
	@Value("${banglalink.url}")
	String baseUrl;
	@Value("${banglalink.user.name}")
	String userName;
	@Value("${banglalink.user.password}")
	String userPassword;
	@Value("${banglalink.max.chunk.size}")
	int maxChunkSize;
	
	@Autowired
	SmsRepository smsRepository;
	@Autowired
	SmsDeliveryStatusRepository deliveryStatusRepo;
	
	static final String primaryOperator = MobileOperatorCode.BANGLALINK.toString();
	private boolean busy = false;
	List<SmsModel> blSmsForDeliveryCheck = new ArrayList<>();	
	
	public void initiateDeliveryChecking() {
		int pendingDlr = smsRepository.pendingDlrCount("BL");
		long startTime = 0;
		if(!busy && pendingDlr > 0) {
			log.info("(" + primaryOperator + ") " + pendingDlr + " Delivery check pending. New cycle can be started");
			startTime = System.currentTimeMillis();
			checkBlDelivery();
			log.info("(" + primaryOperator + ") "+ (System.currentTimeMillis() - startTime) + " ms elapsed for completing delivery check cycle");
		}
		else if (busy && pendingDlr > 0) {
			log.info("(" + primaryOperator + ") " + pendingDlr
					+ " Delivery check pending. Application busy new cycle can not be started");
		} else {
			log.info("(" + primaryOperator + ") " + pendingDlr + " Delivery check pending.");
		}
	}
	
	public void checkBlDelivery() {
		busy =true;
		long startTime = System.currentTimeMillis();
		
		blSmsForDeliveryCheck =  smsRepository.findSentSms("BL", maxChunkSize);
		log.info("(" + primaryOperator + ") Pulled " + blSmsForDeliveryCheck.size() + " SMS for delivery checking in "+ (System.currentTimeMillis() - startTime) + " ms");
		
		updateDeliveryCheckStatus("DLR CHECKING");
		
		List<String> jsonFormattedRequest = checkDeliveryRequestFormatter(blSmsForDeliveryCheck);
			
		callDeliveryApi(jsonFormattedRequest);
		
		blSmsForDeliveryCheck.clear();	
		busy=false;		
	}
	
	
	private void callDeliveryApi(List<String> deliveryRequestJson) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String deliveryUrl = baseUrl;
		long startTime = 0;
		
		ObjectMapper deliveryStatusMapper = new ObjectMapper();
		BlDeliveryResponse smsDeliveryResponse = new BlDeliveryResponse();
		List<SmsDeliveryStatus> saveSmsDelivery = new ArrayList<SmsDeliveryStatus>();

		startTime = System.currentTimeMillis();
		String checkDeliveryResponseJson = "";
		for (int i = 0; i < deliveryRequestJson.size(); i++) {

			HttpEntity<String> httpRequest = new HttpEntity<>(deliveryRequestJson.get(i), headers);
			SmsDeliveryStatus singleStatus = new SmsDeliveryStatus();
			
			try {				
				checkDeliveryResponseJson = restTemplate.postForObject(deliveryUrl, httpRequest, String.class);					
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") " + "Exception while calling check SMS delivery API with request body: "
						+ deliveryRequestJson.get(i) + "  " + e.getMessage());
			}
//			System.out.println(checkDeliveryResponseJson);
			
			try {
				smsDeliveryResponse = deliveryStatusMapper.readValue(checkDeliveryResponseJson, BlDeliveryResponse.class);
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") "
						+ "Exception while decoding send SMS API response with response body: " + checkDeliveryResponseJson
						+ "  " + e.getMessage());
			}			

			singleStatus.setCreateDate(LocalDateTime.now());
			
			singleStatus.setStatusCode(smsDeliveryResponse.getStatusInfo().get(BlCheckDeliveryResponseField.statusCode).toString());
			singleStatus.setStatusDescription(smsDeliveryResponse.getStatusInfo().get(BlCheckDeliveryResponseField.errordescription).toString());
			
//			int st = deliveryRequestJson.get(i).indexOf("clienttransid")+16;
//			int end = deliveryRequestJson.get(i).indexOf("-", st) ;			
//			System.out.println(deliveryRequestJson.get(i).substring(st, end));
//			singleStatus.setSmsId(Integer.parseInt(deliveryRequestJson.get(i).substring(st, end)));
			
			singleStatus.setSmsId(blSmsForDeliveryCheck.get(i).getId());
			singleStatus.setMobileNo(blSmsForDeliveryCheck.get(i).getMobileNo());
			
//			st = deliveryRequestJson.get(i).indexOf("msisdn") + 10;
//			end = deliveryRequestJson.get(i).indexOf("]", st) - 1 ;			
//			System.out.println(deliveryRequestJson.get(i).substring(st, end));
//			singleStatus.setMobileNo(deliveryRequestJson.get(i).substring(st, end));
			
			int st = smsDeliveryResponse.getStatusInfo().get(BlCheckDeliveryResponseField.deliverystatus).toString().indexOf("-") + 1;
			int end = smsDeliveryResponse.getStatusInfo().get(BlCheckDeliveryResponseField.deliverystatus).toString().indexOf("]", st) ;
//			System.out.println(smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.deliverystatus).toString().substring(st, end));
			singleStatus.setDeliveryStatus(smsDeliveryResponse.getStatusInfo().get(BlCheckDeliveryResponseField.deliverystatus).toString().substring(st, end));
					
			saveSmsDelivery.add(singleStatus);
			
			if(smsDeliveryResponse.getStatusInfo().get(BlCheckDeliveryResponseField.statusCode).equals("1000")) {
				blSmsForDeliveryCheck.get(i).setHandsetDelivery(singleStatus.getDeliveryStatus());
			}
			else {
				blSmsForDeliveryCheck.get(i).setHandsetDelivery(smsDeliveryResponse.getStatusInfo().get(BlCheckDeliveryResponseField.errordescription).toString());
			}			
		}		
		log.info("(" + primaryOperator + ") " + deliveryRequestJson.size() + " Delivery calling completed in " + (System.currentTimeMillis() - startTime) + " ms");
		
		try {
			startTime = System.currentTimeMillis();
			deliveryStatusRepo.saveAll(saveSmsDelivery);
			log.info("(" + primaryOperator + ") " + "Inserted "+saveSmsDelivery.size() +" rows in SMS Delivery Status table in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while inserting data in SMS Delivery Status table " + e.getMessage());
		}
		
		try {
			startTime = System.currentTimeMillis();
			smsRepository.saveAll(blSmsForDeliveryCheck);
			log.info("(" + primaryOperator + ") " + "Updated " + blSmsForDeliveryCheck.size()+ " delivery status in SMS Queue table in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating delivery status in SMS Queue table " + e.getMessage());
		}
		
	}

	
	private void updateDeliveryCheckStatus(String status) {
		for (SmsModel deliverySms : blSmsForDeliveryCheck) {
			deliverySms.setHandsetDelivery(status);
		}

		try {
			long startTime = System.currentTimeMillis();
			smsRepository.saveAll(blSmsForDeliveryCheck);
			log.info("(" + primaryOperator + ") " + "Updated sms delivery checking status of those SMS in "
					+ (System.currentTimeMillis() - startTime) + " ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating sms delivery checking status those SMS "
					+ e.getMessage());
		}
	}
	
	
	private List<String> checkDeliveryRequestFormatter (List<SmsModel> smsSentByBl){
		
		BlDeliveryRequest rawDeliveryRequest = new BlDeliveryRequest();					
		List<String> jsonDeliReqBody = new ArrayList<String>();
		ObjectMapper objectMapper = new ObjectMapper();
		
		rawDeliveryRequest.setFixedFields(userName, userPassword, smsSender);
		String[] numberForBl = new String[1];
		for (SmsModel sms : smsSentByBl) {
			rawDeliveryRequest.setOperatortransid(sms.getOperatorId());
			numberForBl[0] = sms.getMobileNo();
			rawDeliveryRequest.setMsisdn(numberForBl);
			rawDeliveryRequest.setClienttransid(sms.getId().toString());
			
			try {
				jsonDeliReqBody.add(objectMapper.writeValueAsString(rawDeliveryRequest));
			} catch (JsonProcessingException e) {
				log.warn("(" + primaryOperator + ") " + "Exception while converting sms Object to JSON for SMS ID : "
						+ sms.getId() + " " + e.getMessage());
			}
		}
		return jsonDeliReqBody;
	}
	
}
