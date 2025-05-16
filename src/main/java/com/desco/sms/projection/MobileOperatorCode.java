package com.desco.sms.projection;

import java.util.ArrayList;

public enum MobileOperatorCode {
	
	GP("GP", new ArrayList<String>() {
		{
			add("88017");
			add("88013");
		}
	}, new ArrayList<String>() {
		{
			add("017");
			add("013");
		}
	}), TELETALK("TT", new ArrayList<String>() {
		{
			add("88015");
		}
	}, new ArrayList<String>() {
		{
			add("015");
		}
	}), ROBI("RB", new ArrayList<String>() {
		{
			add("88016");
			add("88018");
		}
	}, new ArrayList<String>() {
		{
			add("016");
			add("018");
		}
	}), BANGLALINK("BL", new ArrayList<String>() {
		{
			add("88019");
			add("88014");
		}
	}, new ArrayList<String>() {
		{
			add("019");
			add("014");
		}
	});

	private String shortName;
	private ArrayList<String> longCode;
	private ArrayList<String> shortCode;

	private MobileOperatorCode(String shortName, ArrayList<String> longCode, ArrayList<String> shortCode) {
		this.setShortName(shortName);
		this.longCode = longCode;
		this.shortCode = shortCode;		
	}

	public ArrayList<String> getLongCode() {
		return longCode;
	}

	public void setLongCode(ArrayList<String> longCode) {
		this.longCode = longCode;
	}

	public ArrayList<String> getShortCode() {
		return shortCode;
	}

	public void setShortCode(ArrayList<String> shortCode) {
		this.shortCode = shortCode;
	}

	public static MobileOperatorCode match(String s) {
		switch (s) {
		case "TELETALK":
			return TELETALK;
		case "GP":
			return GP;
		case "ROBI":
			return ROBI;
		case "BANGLALINK":
			return BANGLALINK;
		}
		return null;
	}

	
	@Override
	public String toString() {
		switch (this) {
		case TELETALK:
			return "TELETALK";
		case GP:
			return "GP";
		case ROBI:
			return "ROBI";
		case BANGLALINK:
			return "BANGLALINK";
		}
		return null;
	}

	public String getShortName() {
		return shortName;
	}

	public void setShortName(String shortName) {
		this.shortName = shortName;
	}

}
