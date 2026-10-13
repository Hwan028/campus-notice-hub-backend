package com.kkamar.cnh.controller;

import com.kkamar.cnh.exception.ResourceNotFoundException;
import com.kkamar.cnh.service.DeviceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 예외가 어떤 HTTP 상태/본문으로 바뀌는지 검증한다.
 * 이전에는 존재하지 않는 디바이스 요청이 전부 원시 500 에러로 나갔다.
 */
@WebMvcTest(DeviceController.class)
class GlobalExceptionHandlerTest {

    @Autowired private MockMvc mockMvc;
    @MockBean private DeviceService deviceService;

    @Test
    void missingResource_becomes404WithJsonBody() throws Exception {
        when(deviceService.updateDepartment(eq(99L), any()))
                .thenThrow(new ResourceNotFoundException("Device not found: id=99"));

        mockMvc.perform(patch("/api/devices/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"departmentId\":1}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Device not found: id=99"));
    }

    @Test
    void blankDeviceUuid_becomes400() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(containsString("deviceUuid")));
    }

    @Test
    void malformedJson_becomes400() throws Exception {
        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{not json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    void blankKeywordName_becomes400() throws Exception {
        mockMvc.perform(post("/api/devices/1/keywords").param("keywordName", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void illegalArgument_becomes400() throws Exception {
        when(deviceService.registerDevice(any(), any())).thenThrow(new IllegalArgumentException("bad input"));

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"abc\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bad input"));
    }

    @Test
    void wrongHttpMethod_staysA405_notA500() throws Exception {
        // 예전에 겪은 문제: 브라우저 주소창(GET)으로 POST 전용 API를 호출. 포괄 핸들러가 이걸 500으로 바꾸면 안 된다.
        mockMvc.perform(get("/api/devices"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    void unexpectedError_becomes500WithoutLeakingInternals() throws Exception {
        when(deviceService.registerDevice(any(), any())).thenThrow(new RuntimeException("secret internal detail"));

        mockMvc.perform(post("/api/devices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"deviceUuid\":\"abc\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message", not(containsString("secret"))));
    }
}
