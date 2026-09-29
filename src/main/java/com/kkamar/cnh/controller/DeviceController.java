package com.kkamar.cnh.controller;

import com.kkamar.cnh.dto.DeviceRegisterRequest;
import com.kkamar.cnh.dto.DeviceResponse;
import com.kkamar.cnh.service.DeviceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/devices")
public class DeviceController {

    private final DeviceService deviceService;

    @PostMapping
    public DeviceResponse register(@RequestBody DeviceRegisterRequest request) {
        return deviceService.registerDevice(request.getDeviceUuid(), request.getDepartmentId());
    }

    @PatchMapping("/{id}")
    public DeviceResponse updateDepartment(
            @PathVariable Long id,
            @RequestBody DeviceRegisterRequest request
    ) {
        return deviceService.updateDepartment(id, request.getDepartmentId());
    }

    @PostMapping("/{id}/keywords")
    public void subscribeKeyword(
            @PathVariable Long id,
            @RequestParam String keywordName
    ) {
        deviceService.subscribeKeyword(id, keywordName);
    }

    @DeleteMapping("/{id}/keywords/{keywordId}")
    public void unsubscribeKeyword(
            @PathVariable Long id,
            @PathVariable Long keywordId
    ) {
        deviceService.unsubscribeKeyword(id, keywordId);
    }
}