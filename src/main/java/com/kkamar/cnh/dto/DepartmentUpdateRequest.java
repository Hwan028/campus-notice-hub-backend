package com.kkamar.cnh.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

/** PATCH /api/devices/{id} 본문. 등록 요청과 달리 deviceUuid가 없으므로 별도 DTO로 분리했다. */
@Getter
@NoArgsConstructor
public class DepartmentUpdateRequest {

    /** null이면 학과 선택을 해제한다. */
    private Long departmentId;
}
