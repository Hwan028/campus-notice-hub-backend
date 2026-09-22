package com.kkamar.cnh.controller;

import com.kkamar.cnh.dto.NoticeResponse;
import com.kkamar.cnh.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeRepository noticeRepository;

    @GetMapping("/api/notices")
    public List<NoticeResponse> getNotices() {
        return noticeRepository.findAllByOrderByPostedAtDesc().stream()
                .map(NoticeResponse::from)
                .toList();
    }
}