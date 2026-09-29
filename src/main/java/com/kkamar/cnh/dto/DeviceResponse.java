package com.kkamar.cnh.dto;

import com.kkamar.cnh.domain.Device;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceResponse {
    private Long id;
    private String deviceUuid;
    private String departmentName; // 학과 미선택 시 null

    public static DeviceResponse from(Device device) {
        return DeviceResponse.builder()
                .id(device.getId())
                .deviceUuid(device.getDeviceUuid())
                .departmentName(
                        device.getDepartment() == null
                                ? null
                                : device.getDepartment().getName()
                )
                .build();
    }
}