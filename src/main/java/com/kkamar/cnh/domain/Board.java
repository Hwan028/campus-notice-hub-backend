package com.kkamar.cnh.domain;

import com.kkamar.cnh.component.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

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

    @Builder
    public Board(University university, Department department,
                 String name, String crawlUrl) {
        this.university = university;
        this.department = department;
        this.name = name;
        this.crawlUrl = crawlUrl;
        this.active = true;
    }

}
