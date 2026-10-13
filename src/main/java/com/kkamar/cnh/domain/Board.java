package com.kkamar.cnh.domain;

import com.kkamar.cnh.component.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board")
public class Board extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "university_id", nullable = false)
    private University university;

    // null이면 대학 전체 게시판, 값 있으면 학과 게시판
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name="crawl_url", nullable=false)
    private String crawlUrl;

    @Column(name="is_active", nullable = false)
    private boolean active=true;

    /**
     * 크롤러 건강 판정의 기준선을 이 시각 이후의 기록으로만 만든다.
     * 학교가 사이트를 정상적으로 개편해 "새로운 정상"이 생겼을 때(파서를 고친 뒤)
     * 관리자가 기준선을 리셋하는 용도. null이면 전체 기록 사용.
     */
    @Column(name = "baseline_since")
    private LocalDateTime baselineSince;

    @Builder
    public Board(University university, Department department,
                 String name, String crawlUrl) {
        this.university = university;
        this.department = department;
        this.name = name;
        this.crawlUrl = crawlUrl;
        this.active = true;
    }

    public void resetBaseline(LocalDateTime at) {
        this.baselineSince = at;
    }
}
