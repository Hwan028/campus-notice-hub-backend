package com.kkamar.cnh.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeviceRegisterRequest {
    private String deviceUuid;
    private Long departmentId; // 학과를 아직 안 골랐으면 null로 보내면 됨
}