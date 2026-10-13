package com.kkamar.cnh.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeviceRegisterRequest {

    @NotBlank
    @Size(max = 64)
    private String deviceUuid;

    private Long departmentId; // 학과를 아직 안 골랐으면 null로 보내면 됨
}
