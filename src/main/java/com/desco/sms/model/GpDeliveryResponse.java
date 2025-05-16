package com.desco.sms.model;

import java.util.EnumMap;

import com.desco.sms.projection.GpCheckDeliveryResponseField;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GpDeliveryResponse {
	EnumMap<GpCheckDeliveryResponseField, Object> statusInfo = new EnumMap<>(GpCheckDeliveryResponseField.class);
}
