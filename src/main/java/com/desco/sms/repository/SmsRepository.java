package com.desco.sms.repository;

import java.util.ArrayList;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.desco.sms.model.SmsModel;


public interface SmsRepository extends JpaRepository<SmsModel, Integer>{
	String smsQuery = "WITH RankedSMS AS ("
			+ "    SELECT SQ.ID, SQ.SMS_TEXT, SQ.MOBILE_NO, SQ.SENT_STATUS, SQ.SENT_DATE, SQ.OPERATOR_ID, SQ.HANDSET_DELIVERY, SQ.SENDER_EID, SQ.TRACKING_NUMBER, ROW_NUMBER() OVER (PARTITION BY SQ.MOBILE_NO ORDER BY SQ.ID) "
			+ "    AS full_tbl FROM SHOHEL.SMS_QUEUE_TBL_TEST SQ WHERE trunc(sysdate) - trunc(SQ.CREATE_DATE) <=:smsAge and SQ.SENT_STATUS = 'N' AND "
			+ "            (               "
			+ "                (LENGTH ( TRIM(SQ.MOBILE_NO) ) = 13 AND SUBSTR ( TRIM (SQ.MOBILE_NO), 1, 5) IN (:operatorCodeLong) ) "
			+ "                OR "
			+ "                (LENGTH ( TRIM(SQ.MOBILE_NO) ) = 11 AND SUBSTR ( TRIM (SQ.MOBILE_NO), 1, 3) IN (:operatorCodeShort) )"
			+ "            ) "
			+ "   ) "
			+ " SELECT RankedSMS.ID, RankedSMS.SMS_TEXT, RankedSMS.MOBILE_NO, RankedSMS.SENT_STATUS, RankedSMS.SENT_DATE, RankedSMS.OPERATOR_ID, RankedSMS.HANDSET_DELIVERY, "
			+ " RankedSMS.SENDER_EID, RankedSMS.TRACKING_NUMBER FROM RankedSMS WHERE RankedSMS.full_tbl = 1 AND ROWNUM <= :rowLimit ORDER BY RankedSMS.ID ASC";
//	FETCH NEXT :rowLimit ROWS ONLY
	@Query(value = smsQuery, nativeQuery = true)
	List<SmsModel> findUnsentSms(ArrayList<String> operatorCodeLong, ArrayList<String> operatorCodeShort, int rowLimit, int smsAge);

	
	String pendingSmsQuery = "select count(mobile_no)  from SHOHEL.SMS_QUEUE_TBL_TEST where sent_status = 'N' and trunc(sysdate) - trunc(CREATE_DATE) <=:smsAge and"
			+ "                 ("
			+ "                   (LENGTH ( TRIM(MOBILE_NO) ) = 13 AND SUBSTR ( TRIM (MOBILE_NO), 1, 5) IN (:operatorCodeLong) )"
			+ "                  OR "
			+ "                    (LENGTH ( TRIM(MOBILE_NO) ) = 11 AND SUBSTR ( TRIM (MOBILE_NO), 1, 3) IN (:operatorCodeShort) )"
			+ "                 )";
	@Query(value = pendingSmsQuery, nativeQuery = true)
	int pendingSmsCount(ArrayList<String> operatorCodeLong, ArrayList<String> operatorCodeShort, int smsAge);
	
	
//	@Query("SELECT new com.desco.sms.projection.DeliverySmsModel(id, mobileNo, operatorId, sentStatus, senderId, trackingNumber) FROM SmsModel WHERE senderId =?1AND trackingNumber='9914010532' AND ROWNUM <= ?2 ORDER BY id ASC")
//	@Query("SELECT id, mobileNo, smsText, sentDate, sentStatus, operatorId, handsetDelivery, senderId, trackingNumber FROM SmsModel WHERE sentStatus <>'N' AND senderId =:senderId AND trackingNumber='9914010532' AND ROWNUM <=:rowLimit ORDER BY id ASC")
	@Query("SELECT sq FROM SmsModel sq WHERE sentStatus <> 'N' AND senderId =:senderId AND handsetDelivery = 'DLR PENDING' AND ROWNUM <=:rowLimit ORDER BY id ASC")
	List<SmsModel> findSentSms(String senderId, int rowLimit);
	
	
	@Query("SELECT COUNT(id) FROM SmsModel WHERE senderId=:senderEid AND handsetDelivery = 'DLR PENDING' AND sentStatus <>'N' ")
	int pendingDlrCount(String senderEid);
	
	
	/*@Modifying()
	@Transactional
	@Query("UPDATE SmsModel SET sentStatus = 'P', senderId=:senderId WHERE ID IN (:id)")
	int updateSentStatus(String senderId, List<Integer> id );

	@Query("UPDATE SmsModel SET sentStatus = 'Y', sentDate =:sentTime, operatorId=:telecomId WHERE ID IN (:id)")
	@Modifying
	@Transactional
	int updateSmsStatus(int id, LocalDateTime sentTime, String telecomId);*/


}
