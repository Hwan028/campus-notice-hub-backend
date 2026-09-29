package com.kkamar.cnh.service;

import com.kkamar.cnh.dto.DeviceResponse;

public interface DeviceService {
    DeviceResponse registerDevice(String deviceUuid, Long departmentId);
    DeviceResponse updateDepartment(Long deviceId, Long departmentId);
    void subscribeKeyword(Long deviceId, String keywordName);
    void unsubscribeKeyword(Long deviceId, Long keywordId);
}