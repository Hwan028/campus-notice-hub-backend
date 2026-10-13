package com.kkamar.cnh.controller;

import com.kkamar.cnh.dto.ScrapRequest;
import com.kkamar.cnh.dto.ScrapResponse;
import com.kkamar.cnh.service.ScrapService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/scraps")
public class ScrapController {

    private final ScrapService scrapService;

    @PostMapping
    public ScrapResponse scrap(@Valid @RequestBody ScrapRequest request) {
        return scrapService.scrap(request.getDeviceId(), request.getNoticeId(), request.getColorIndex());
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unscrap(@RequestParam Long deviceId, @RequestParam Long noticeId) {
        scrapService.unscrap(deviceId, noticeId);
    }

    @GetMapping
    public List<ScrapResponse> getScraps(@RequestParam Long deviceId) {
        return scrapService.getScraps(deviceId);
    }
}
