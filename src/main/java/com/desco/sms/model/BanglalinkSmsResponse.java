package com.desco.sms.model;

import java.util.EnumMap;

import com.desco.sms.projection.BanglalinkSendSmsResponseField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class BanglalinkSmsResponse {
	EnumMap<BanglalinkSendSmsResponseField, String> statusInfo = new EnumMap<>(BanglalinkSendSmsResponseField.class); 
}
