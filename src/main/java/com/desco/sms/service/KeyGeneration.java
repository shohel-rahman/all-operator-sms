package com.desco.sms.service;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

//import org.apache.tomcat.util.net.openssl.ciphers.MessageDigest;

import lombok.extern.log4j.Log4j2;

@Log4j2
public class KeyGeneration {
	private static final int userID = 35;

	public String getPKey(long currentTime) {
		// unique 16 digit number
		String P_KEY = "100" + currentTime;
		return P_KEY;
	}

	public String getAKey(long currentTime) {
		String A_KEY = getMd5("100" + (currentTime + userID) + "****");
		return A_KEY;
	}

	public static String getMd5(String input) {
		try {

			// Static getInstance method is called with hashing MD5
			MessageDigest md = MessageDigest.getInstance("MD5");
//					getInstance("MD5");

			// digest() method is called to calculate message digest
			// of an input digest() return array of byte
			byte[] messageDigest = md.digest(input.getBytes());

			// Convert byte array into signum representation
			BigInteger no = new BigInteger(1, messageDigest);

			// Convert message digest into hex value
			String hashtext = no.toString(16);
			while (hashtext.length() < 32) {
				hashtext = "0" + hashtext;
			}
			return hashtext;
		}

		// For specifying wrong message digest algorithms
		catch (NoSuchAlgorithmException e) {
			log.warn("Exception while calculating MD5 value : "+e.getMessage());
			throw new RuntimeException(e);
		}
	}
}
