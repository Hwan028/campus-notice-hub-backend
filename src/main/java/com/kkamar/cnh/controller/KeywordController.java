package com.kkamar.cnh.controller;

import com.kkamar.cnh.dto.KeywordResponse;
import com.kkamar.cnh.repository.KeywordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class KeywordController {

    private final KeywordRepository keywordRepository;

    @GetMapping("/api/keywords")
    public List<KeywordResponse> getKeywords() {
        return keywordRepository.findAll().stream()
                .map(KeywordResponse::from)
                .toList();
    }
}