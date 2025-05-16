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

import com.desco.sms.model.GpSingleSmsRequest;
import com.desco.sms.model.GpSmsResponse;
import com.desco.sms.model.SmsModel;
import com.desco.sms.projection.GpSendSmsResponseField;
import com.desco.sms.projection.MobileOperatorCode;
import com.desco.sms.repository.SmsRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class GpService {

	@Value("${sms.maximum.age}")
	int smsMaxAge;	
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
	@Value("${gp.additional.operator.active}")
	private boolean isAdditionalOperatorActive;
	@Value("${gp.additional.operators}")
	private String[] additionalOperators;

	@Autowired
	SmsRepository smsRepository;

	static final String primaryOperator = MobileOperatorCode.GP.toString();
	ArrayList<String> longCodes = new ArrayList<String>();
	ArrayList<String> shortCodes = new ArrayList<String>();

	private boolean busy = false;

	@PostConstruct
	public void generateNumberFormat(){
		longCodes = MobileOperatorCode.GP.getLongCode();
		shortCodes = MobileOperatorCode.GP.getShortCode();
		if (isAdditionalOperatorActive) {
			additionalOperatorChecker();
		}		
	}
	
	public void operateGp() {		

		long startTime = 0;
		int pendingSms = smsRepository.pendingSmsCount(longCodes, shortCodes, smsMaxAge);
//		log.info("(" + primaryOperator + ") "+ (System.currentTimeMillis() - startTime) + "ms elapsed for pending sms count");
		
		if (!busy && pendingSms > 0) {
			log.info("(" + primaryOperator + ") " + pendingSms + " SMS pending. New cycle can be started");
			startTime = System.currentTimeMillis();
			initiateSmsSending();
			log.info("(" + primaryOperator + ") "+ (System.currentTimeMillis() - startTime) + " ms elapsed for completing full cycle");
		} else if (busy && pendingSms > 0) {
			log.info("(" + primaryOperator + ") " + pendingSms
					+ " SMS pending. Application busy new cycle can not be started");
		} else {
			log.info("(" + primaryOperator + ") " + pendingSms + " SMS pending.");
		}
	}

	private void initiateSmsSending() {
		busy = true;
		long startTime = 0;
		int pulledSmsCount = 0;
		log.info("(" + primaryOperator + ") " + "SMS pulling started");
		startTime = System.currentTimeMillis();
		List<SmsModel> pulledSmsList = getSms();		
		pulledSmsCount = pulledSmsList.size();
		log.info("(" + primaryOperator + ") " + pulledSmsCount + " SMS pulled from DB in "+ (System.currentTimeMillis() - startTime) + " ms");

		log.info("(" + primaryOperator + ") " + "Process of updating sentStatus(P), senderId, sentDate of " + pulledSmsCount + " SMS started");
		startTime = System.currentTimeMillis();
		updatePullStatus(pulledSmsList);
		log.info("(" + primaryOperator + ") " + "Process of updating sentStatus(P), senderId, sentDate ended in " + (System.currentTimeMillis() - startTime) + " ms");
		
		log.info("(" + primaryOperator + ") " + "Process of sending " + pulledSmsCount + " SMS started");
//		startTime = System.currentTimeMillis();
		sendSms(pulledSmsList);
//		log.info("(" + primaryOperator + ") " + "Process of sending SMS ended in " + (System.currentTimeMillis() - startTime) + " ms");

		busy = false;
	}

	private void sendSms(List<SmsModel> smsList) {
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		String sendSmsUrl = baseUrl;
		long startTime = 0;

		List<String> smsJson = sendSmsRequestFormatter(smsList);
		ObjectMapper smsResponseMapper = new ObjectMapper();
		GpSmsResponse gpSmsResponse = new GpSmsResponse();

		startTime = System.currentTimeMillis();
		for (int i = 0; i < smsJson.size(); i++) {

			HttpEntity<String> httpRequest = new HttpEntity<>(smsJson.get(i), headers);
			String sendSmsResponseJson = "";
			try {				
				sendSmsResponseJson = restTemplate.postForObject(sendSmsUrl, httpRequest, String.class);				
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") " + "Exception while calling send SMS API with request body: "
						+ smsJson.get(i) + "  " + e.getMessage());
			}

			try {
				gpSmsResponse = smsResponseMapper.readValue(sendSmsResponseJson, GpSmsResponse.class);
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") "
						+ "Exception while decoding send SMS API response with response body: " + sendSmsResponseJson
						+ "  " + e.getMessage());
			}
			
			smsList.get(i).setSentDate(LocalDateTime.now());
			smsList.get(i).setOperatorId(gpSmsResponse.getStatusInfo().get(GpSendSmsResponseField.serverReferenceCode));
			
			if(gpSmsResponse.getStatusInfo().get(GpSendSmsResponseField.statusCode).equals("1000")) {
				smsList.get(i).setSentStatus(gpSmsResponse.getStatusInfo().get(GpSendSmsResponseField.errordescription));				
				smsList.get(i).setHandsetDelivery("DLR PENDING");
			}
			else {					
				smsList.get(i).setSentStatus(gpSmsResponse.getStatusInfo().get(GpSendSmsResponseField.statusCode));				
				smsList.get(i).setHandsetDelivery(gpSmsResponse.getStatusInfo().get(GpSendSmsResponseField.errordescription));
			}			
		}		
		log.info("(" + primaryOperator + ") " + smsList.size() + " SMS send API calling completed in " + (System.currentTimeMillis() - startTime) + " ms");
		
		try {
			startTime = System.currentTimeMillis();
			smsRepository.saveAll(smsList);
			log.info("(" + primaryOperator + ") " + "Updated sent_status, sent_date, operator_id, handset_delivery(DLR PENDING) of those SMS in "+ (System.currentTimeMillis() - startTime) +" ms");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating sent_status, sent_date, operator_id, handset_delivery(DLR PENDING) in SMS_QUEUE table " + e.getMessage());
		}
	}

	private List<String> sendSmsRequestFormatter(List<SmsModel> rawSmsBody) {

		List<String> jsonSmsBody = new ArrayList<String>();
		GpSingleSmsRequest gpSms = new GpSingleSmsRequest();
		String[] numberForGp = new String[1];
		ObjectMapper objectMapper = new ObjectMapper();
		
		
		gpSms.setFixedFields(userName, userPassword, smsSender);
		for (int i = 0; i < rawSmsBody.size(); i++) {

			gpSms.setMessage(rawSmsBody.get(i).getSmsText());
			gpSms.setClienttransid(rawSmsBody.get(i).getId().toString());
			numberForGp[0] = rawSmsBody.get(i).getMobileNo();
			gpSms.setMsisdn(numberForGp);

			try {
				jsonSmsBody.add(objectMapper.writeValueAsString(gpSms));
			} catch (JsonProcessingException e) {
				log.warn("(" + primaryOperator + ") " + "Exception while converting sms Object to JSON for SMS ID : "
						+ rawSmsBody.get(i).getId() + " " + e.getMessage());
			}
		}

		return jsonSmsBody;
	}

	private void additionalOperatorChecker() {
		log.info("(" + primaryOperator + ") " + "Additional operator is active ");
		log.info("Additional operators are: ");
		for (String operator : additionalOperators) {
			log.info(operator);
			longCodes.addAll(MobileOperatorCode.match(operator).getLongCode());
			shortCodes.addAll(MobileOperatorCode.match(operator).getShortCode());
		}
	}

	private List<SmsModel> getSms() {
		List<SmsModel> pulledSmsList = new ArrayList<>();
		try {
			pulledSmsList = smsRepository.findUnsentSms(longCodes, shortCodes, maxChunkSize, smsMaxAge);
			log.info("(" + primaryOperator + ") " + "SMS pulling successfull.");
			return pulledSmsList;
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while getting SMS from DB " + e.getMessage());
			return pulledSmsList;
		}
	}

	private void updatePullStatus(List<SmsModel> smsList) {
		for (SmsModel sms : smsList) {
			sms.setSenderId("GP");
			sms.setSentStatus("P");
			sms.setSentDate(LocalDateTime.now());
		}
		try {
			smsRepository.saveAll(smsList);
			log.info("(" + primaryOperator + ") " + "sentStatus(P), senderId, sentDate updated of those rows");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating sentStatus(P), senderId, sentDate " + e.getMessage());
		}
	}
}
