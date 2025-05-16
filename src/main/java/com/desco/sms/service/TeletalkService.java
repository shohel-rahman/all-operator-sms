package com.desco.sms.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.desco.sms.model.SmsModel;
import com.desco.sms.model.TeleBulkResponseModel;
import com.desco.sms.projection.MobileOperatorCode;
import com.desco.sms.projection.TeleSendSmsRequest;
import com.desco.sms.repository.SmsRepository;
import com.desco.sms.repository.TeleBulkResponseRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;

@Service
@Log4j2
public class TeletalkService {

	@Value("${sms.maximum.age}")
	int smsMaxAge;
	@Value("${teletalk.url}")
	String baseUrl;
	@Value("${teletalk.user.name}")
	String userName;
	@Value("${teletalk.user.password}")
	String userPassword;
	@Value("${teletalk.max.chunk.size}")
	int maxChunkSize;
	@Value("${teletalk.additional.operator.active}")
	private boolean isAdditionalOperatorActive;
	@Value("${teletalk.additional.operators}")
	private String[] additionalOperators;

	@Autowired
	SmsRepository smsRepository;

	@Autowired
	TeleBulkResponseRepository teleBulkResponseRep;

	static final String primaryOperator = MobileOperatorCode.TELETALK.toString();
	ArrayList<String> longCodes = new ArrayList<String>();
	ArrayList<String> shortCodes = new ArrayList<String>();

	private boolean busy = false;

	@PostConstruct
	public void generateNumberFormat(){
		longCodes = MobileOperatorCode.TELETALK.getLongCode();
		shortCodes = MobileOperatorCode.TELETALK.getShortCode();
		if (isAdditionalOperatorActive) {
			additionalOperatorChecker();
		}		
	}
	
	public void operateTeletalk() {
		
		long startTime= 0;
		int pendingSms = smsRepository.pendingSmsCount(longCodes, shortCodes, smsMaxAge);
		if (!busy && pendingSms > 0) {
			log.info("(" + primaryOperator + ") " + pendingSms + " SMS pending. New cycle can be started");
			startTime = System.currentTimeMillis();
			initiateSmsSending();
			log.info("(" + primaryOperator + ") "+ (System.currentTimeMillis() - startTime) + " ms elapsed for completing full cycle");
		} else if (busy && pendingSms > 0) {
			log.info("(" + primaryOperator + ") " + pendingSms + " SMS pending. Application busy new cycle can not be started");
		} else {
			log.info("(" + primaryOperator + ") " + pendingSms + " SMS pending.");
		}
	}

	private void additionalOperatorChecker() {
		log.info("Additional operator is active for " + primaryOperator);
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
			pulledSmsList = smsRepository.findUnsentSms(longCodes, shortCodes, maxChunkSize*10, smsMaxAge);
			log.info("(" + primaryOperator + ") " + "SMS pulling successfull.");
			return pulledSmsList;
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while getting SMS from DB " + e.getMessage());
			return pulledSmsList;
		}
	}

	private void updatePullStatus(List<SmsModel> smsList) {
		for (SmsModel sms : smsList) {
			sms.setSenderId("TT");
			sms.setSentStatus("P");
		}
		try {
			smsRepository.saveAll(smsList);
			log.info("(" + primaryOperator + ") " + "Sent status updated with P of those rows");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception at sent/pull status update " + e.getMessage());
		}
	}

	private void initiateSmsSending() {
		busy = true;

		int pulledSmsCount = 0;
		long startTime = 0;
		log.info("(" + primaryOperator + ") " + "SMS pulling started");
		startTime = System.currentTimeMillis();
		List<SmsModel> pulledSmsList = getSms();
		pulledSmsCount = pulledSmsList.size();
		log.info("(" + primaryOperator + ") " + pulledSmsCount + " SMS pulled from DB in "+ (System.currentTimeMillis() - startTime) + " ms");

		log.info("(" + primaryOperator + ") " + "Process of updating sender ID, pull status of " + pulledSmsCount + " SMS started");
		startTime = System.currentTimeMillis();
		updatePullStatus(pulledSmsList);
		log.info("(" + primaryOperator + ") " + "Process of updating sender ID, pull status ended in " + (System.currentTimeMillis() - startTime) + " ms");

		log.info("(" + primaryOperator + ") " + "Process of sending " + pulledSmsCount + " SMS started");
		sendBulkSms(pulledSmsList);
		log.info("(" + primaryOperator + ") " + "Process of sending " + pulledSmsCount + " SMS ended");

		busy = false;
	}

	private void sendBulkSms(List<SmsModel> pulledSmsList) {
		long chunkStartTime = 0; 
		long batchStartTime = System.currentTimeMillis();
		
		while (!pulledSmsList.isEmpty()) {
			chunkStartTime = System.currentTimeMillis();
			int upperLimit = maxChunkSize < pulledSmsList.size() ? maxChunkSize : pulledSmsList.size();
			List<SmsModel> chunkedList = pulledSmsList.subList(0, upperLimit);
			log.info("(" + primaryOperator + ") " + upperLimit + " SMS extracted for Teletalk API call");

			String apiRequestJson = sendSmsApiRequestFormatter(chunkedList);
			TeleBulkResponseModel receivedResponse = callSmsSendApi(apiRequestJson);

			// Extracting the individual SMS sent status from response body
			Queue<String> individualSentStatus = teleResponseProcessingDetailsFieldDecoder(
					receivedResponse.getProcessing_details());

			// Extracting the individual identifier for sent SMS from response body
			Queue<String> individualSentIdentifier = teleResponseDetailsFieldDecoder(receivedResponse.getDetails());

			try {
				receivedResponse.setDetails(trimResponseDetail(receivedResponse.getDetails()));
				receivedResponse.setProcessing_details(trimResponseDetail(receivedResponse.getProcessing_details()));
				receivedResponse.setCreate_date(LocalDateTime.now());

				teleBulkResponseRep.save(receivedResponse);
				log.info("(" + primaryOperator + ") " + "Received bulk response successfully saved in DB");
			} catch (Exception e) {
				log.warn("(" + primaryOperator + ") " + "Exception while saving the received bulk response in DB " + e.getMessage());
			}

			int updatedSmsRows = updateSmsStatus(chunkedList, individualSentStatus, individualSentIdentifier);
			log.info("(" + primaryOperator + ") " + updatedSmsRows + " SMS rows have been updated.");

			chunkedList.clear();
			log.info("(" + primaryOperator + ") " + "Process for the chunk with " +upperLimit+ " SMS completed in " + (System.currentTimeMillis() - chunkStartTime) + " ms");
		}
		log.info("(" + primaryOperator + ") " + "Process for the batch with " +pulledSmsList.size()+ " SMS completed in " + (System.currentTimeMillis() - batchStartTime) + " ms");
		
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
			smsRepository.saveAll(smsList);
			log.info("(" + primaryOperator + ") " + "Updated sent_status, sent_date, hadset_delivery, operator_id for each extracted SMS");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while updating each extracted SMS " + e.getMessage());
		}

		return smsList.size();
	}

	//============== Fully Teletalk dependent codes start from here ===================================

	private String trimResponseDetail(String large) {
		return large.substring(0, large.indexOf("|"));
	}

	private Queue<String> teleResponseDetailsFieldDecoder(String line) {
		Queue<String> individualResponseId = new LinkedList<>();
		int start = line.indexOf("=");
		int end = line.indexOf("|", start);
		int noOfRecords = 0;
		if (start > 0 && end > start) {
			noOfRecords = Integer.parseInt(line.substring(start + 1, end));
			for (int i = 0; i < noOfRecords; i++) {
				start = line.indexOf("ID=", end) + 3;
				end = line.indexOf(",", start);
				individualResponseId.add(line.substring(start, end));
			}
		}
		return individualResponseId;
	}

	private Queue<String> teleResponseProcessingDetailsFieldDecoder(String line) {
		Queue<String> individualResponseCode = new LinkedList<>();
		int start = line.indexOf("=");
		int end = line.indexOf("|", start);
		int noOfRecords = 0;
		if (start > 0 && end > start) {
			noOfRecords = Integer.parseInt(line.substring(start + 1, end));
			for (int i = 1; i < noOfRecords; i++) {
				start = end;
				end = line.indexOf("|", start + 1);
				individualResponseCode.add(line.substring(start + 1, end));
			}
		}
		individualResponseCode.add(line.substring(end + 1, line.length()));
		return individualResponseCode;
	}

	private TeleBulkResponseModel callSmsSendApi(String requestJson) {

		// Setting up the HTTP Request Header
		RestTemplate restTemplate = new RestTemplate();
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		// Post For Object Method
		HttpEntity<String> httpRequest = new HttpEntity<>(requestJson, headers);
		TeleBulkResponseModel bulkResponse = new TeleBulkResponseModel();
		ObjectMapper responseJsonMapper = new ObjectMapper();
		String teletalkResponseJson = "";

		try {
			log.info("(" + primaryOperator + ") " + "Strat Teletalk API calling");
			teletalkResponseJson = restTemplate.postForObject(baseUrl, httpRequest, String.class);
			log.info("(" + primaryOperator + ") " + "Resoponse received");
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while calling TBL SMS API " + e.getMessage());
		}

		try {
			log.info("(" + primaryOperator + ") " + "Strat decoding Teletalk API response");
			bulkResponse = responseJsonMapper.readValue(teletalkResponseJson, TeleBulkResponseModel.class);
			return bulkResponse;
		} catch (Exception e) {
			log.warn("(" + primaryOperator + ") " + "Exception while decoding TBL SMS response " + e.getMessage());
			return bulkResponse;
		}
	}

	private String sendSmsApiRequestFormatter(List<SmsModel> smsList) {
		int noOfSms = smsList.size();
		String mobileNoList = "";
		String smsTextList = "";
		String cId = "";
		String jsonRequestBody = "";
		String firstId = Integer.toString(smsList.get(0).getId());

		Long currentTime = System.currentTimeMillis();
		KeyGeneration keyGen = new KeyGeneration();

		for (int i = 0; i < noOfSms; i++) {
			if (i == 0) {
				mobileNoList += smsList.get(i).getMobileNo();
				smsTextList += smsList.get(i).getSmsText();
			} else {
				mobileNoList += "|" + smsList.get(i).getMobileNo();
				smsTextList += "|" + smsList.get(i).getSmsText();
			}
		}
		String lastId = Integer.toString(smsList.get(noOfSms - 1).getId());
		cId += firstId + lastId;

		TeleSendSmsRequest requestBody = new TeleSendSmsRequest();

		requestBody.setOp("SMS");
		requestBody.setUser(userName);
		requestBody.setPass(userPassword);
		requestBody.setChunk("V");
		requestBody.setCid(cId);
		requestBody.setServerName("bulksms1.teletalk.com.bd");
		requestBody.setSmsClass("GENERAL");
		requestBody.setValidity("720");
		requestBody.setMobile(mobileNoList);
		requestBody.setSms(smsTextList);
		requestBody.setCharset("ASCII", noOfSms);

		requestBody.setP_key(keyGen.getPKey(currentTime));
		requestBody.setA_key(keyGen.getAKey(currentTime));

		ObjectMapper objectMapper = new ObjectMapper();
		try {
			jsonRequestBody = objectMapper.writeValueAsString(requestBody);
			log.info("(" + primaryOperator + ") " + "SMS JSON is generated");
		} catch (JsonProcessingException e) {
			log.warn("(" + primaryOperator + ") " + "Exception while converting sms Object to JSON : " + e.getMessage());
		}
		return jsonRequestBody;
	}

}
