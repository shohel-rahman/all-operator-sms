package com.desco.sms.model;

import java.util.EnumMap;
import com.desco.sms.projection.GpSendSmsResponseField;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
public class GpSmsResponse {
	EnumMap<GpSendSmsResponseField, String> statusInfo = new EnumMap<>(GpSendSmsResponseField.class); 
}
