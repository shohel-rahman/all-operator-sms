package com.desco.sms.model;

import java.util.EnumMap;

import com.desco.sms.projection.BlCheckDeliveryResponseField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BlDeliveryResponse {
	EnumMap<BlCheckDeliveryResponseField, Object> statusInfo = new EnumMap<>(BlCheckDeliveryResponseField.class);
}
