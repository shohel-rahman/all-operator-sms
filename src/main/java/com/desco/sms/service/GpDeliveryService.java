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

import com.desco.sms.model.GpDeliveryRequest;
import com.desco.sms.model.GpDeliveryResponse;
import com.desco.sms.model.SmsDeliveryStatus;
import com.desco.sms.model.SmsModel;
import com.desco.sms.projection.GpCheckDeliveryResponseField;
import com.desco.sms.projection.MobileOperatorCode;
import com.desco.sms.repository.SmsDeliveryStatusRepository;
import com.desco.sms.repository.SmsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class GpDeliveryService {
	@Value("${gp.sender}")
	String smsSender;
	@Value("${gp.url}")
	String baseUrl;
	@Value("${gp.user.name}")
	String userName;
	@Value("${gp.user.password}")
	String userPassword;
	@Value("${gp.max.chunk.size}")
	int maxChunkSize;
	
	@Autowired
	SmsRepository smsRepository;
	@Autowired
	SmsDeliveryStatusRepository gpDeliveryRepo;
	
	static final String primaryOperator = MobileOperatorCode.GP.toString();
	private boolean busy = false;
		
	List<SmsModel> gpSmsForDeliveryCheck = new ArrayList<>();		
	
	public void initiateDeliveryChecking() {
		int pendingDlr = smsRepository.pendingDlrCount("GP");
		long startTime = 0;
		if(!busy && pendingDlr > 0) {
			log.info("(" + primaryOperator + ") " + pendingDlr + " Delivery check pending. New cycle can be started");
			startTime = System.currentTimeMillis();
			checkGpDelivery();
			log.info("(" + primaryOperator + ") "+ (System.currentTimeMillis() - startTime) + " ms elapsed for completing delivery check cycle");
		}
		else if (busy && pendingDlr > 0) {
			log.info("(" + primaryOperator + ") " + pendingDlr
					+ " Delivery check pending. Application busy new cycle can not be started");
		} else {
			log.info("(" + primaryOperator + ") " + pendingDlr + " Delivery check pending.");
		}
	}
	
	public void checkGpDelivery() {
		busy =true;
		long startTime = System.currentTimeMillis();
		
		gpSmsForDeliveryCheck =  smsRepository.findSentSms("GP", maxChunkSize);
		log.info("(" + primaryOperator + ") Pulled " + gpSmsForDeliveryCheck.size() + " SMS for delivery checking in "+ (System.currentTimeMillis() - startTime) + " ms");
		
		updateDeliveryCheckStatus("DLR CHECKING");
		
		List<String> jsonFormattedRequest = checkDeliveryRequestFormatter(gpSmsForDeliveryCheck);
			
		callDeliveryApi(jsonFormattedRequest);
		
		gpSmsForDeliveryCheck.clear();	
		busy=false;		
	}
	
	private void callDeliveryApi(List<String> deliveryRequestJson) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String deliveryUrl = baseUrl;
		long startTime = 0;
		
		ObjectMapper deliveryStatusMapper = new ObjectMapper();
		GpDeliveryResponse smsDeliveryResponse = new GpDeliveryResponse();
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
				smsDeliveryResponse = deliveryStatusMapper.readValue(checkDeliveryResponseJson, GpDeliveryResponse.class);
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") "
						+ "Exception while decoding send SMS API response with response body: " + checkDeliveryResponseJson
						+ "  " + e.getMessage());
			}			

			singleStatus.setCreateDate(LocalDateTime.now());
			
			singleStatus.setStatusCode(smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.statusCode).toString());
			singleStatus.setStatusDescription(smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.errordescription).toString());
			
			int st = deliveryRequestJson.get(i).indexOf("clienttransid")+16;
			int end = deliveryRequestJson.get(i).indexOf("-", st) ;			
//			System.out.println(deliveryRequestJson.get(i).substring(st, end));
			singleStatus.setSmsId(Integer.parseInt(deliveryRequestJson.get(i).substring(st, end)));
			
			st = deliveryRequestJson.get(i).indexOf("msisdn") + 10;
			end = deliveryRequestJson.get(i).indexOf("]", st) - 1 ;			
//			System.out.println(deliveryRequestJson.get(i).substring(st, end));
			singleStatus.setMobileNo(deliveryRequestJson.get(i).substring(st, end));
			
			st = smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.deliverystatus).toString().indexOf("-") + 1;
			end = smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.deliverystatus).toString().indexOf("]", st) ;
//			System.out.println(smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.deliverystatus).toString().substring(st, end));
			singleStatus.setDeliveryStatus(smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.deliverystatus).toString().substring(st, end));
					
			saveSmsDelivery.add(singleStatus);
			
			if(smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.statusCode).equals("1000")) {
				gpSmsForDeliveryCheck.get(i).setHandsetDelivery(singleStatus.getDeliveryStatus());
			}
			else {
				gpSmsForDeliveryCheck.get(i).setHandsetDelivery(smsDeliveryResponse.getStatusInfo().get(GpCheckDeliveryResponseField.errordescription).toString());
			}			
		}		
		log.info("(" + primaryOperator + ") " + deliveryRequestJson.size() + " Delivery calling completed in " + (System.currentTimeMillis() - startTime) + " ms");
		
		try {
			startTime = System.currentTimeMillis();
			gpDeliveryRepo.saveAll(saveSmsDelivery);
			log.info("(" + primaryOperator + ") " + "Inserted "+saveSmsDelivery.size() +" rows in SMS Delivery Status table in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while inserting data in SMS Delivery Status table " + e.getMessage());
		}
		
		try {
			startTime = System.currentTimeMillis();
			smsRepository.saveAll(gpSmsForDeliveryCheck);
			log.info("(" + primaryOperator + ") " + "Updated " + gpSmsForDeliveryCheck.size()+ " delivery status in SMS Queue table in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating delivery status in SMS Queue table " + e.getMessage());
		}
		
	}
	
	
	private void updateDeliveryCheckStatus (String status) {
		for (SmsModel deliverySms : gpSmsForDeliveryCheck) {
			deliverySms.setHandsetDelivery(status);
		}
		try {
			long startTime = System.currentTimeMillis();
			smsRepository.saveAll(gpSmsForDeliveryCheck);
			log.info("(" + primaryOperator + ") " + "Updated sms delivery checking status of those SMS in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating sms delivery checking status those SMS " + e.getMessage());
		}
	}
	
	
	private List<String> checkDeliveryRequestFormatter (List<SmsModel> smsSentByGp){
		
		GpDeliveryRequest rawDeliveryRequest = new GpDeliveryRequest();					
		List<String> jsonDeliReqBody = new ArrayList<String>();
		ObjectMapper objectMapper = new ObjectMapper();
		
		rawDeliveryRequest.setFixedFields(userName, userPassword, smsSender);
		String[] numberForGp = new String[1];
		for (SmsModel sms : smsSentByGp) {
			rawDeliveryRequest.setOperatortransid(sms.getOperatorId());
			numberForGp[0] = sms.getMobileNo();
			rawDeliveryRequest.setMsisdn(numberForGp);
			rawDeliveryRequest.setClienttransid(sms.getId().toString());
//			System.out.println(rawDeliveryRequest);
			
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
