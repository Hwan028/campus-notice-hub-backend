package com.kkamar.cnh.domain;

import com.kkamar.cnh.component.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Getter
@NoArgsConstructor
@Table(name="notice", uniqueConstraints = {
        @UniqueConstraint(name="uk_board_external_id", columnNames = {"board_id", "external_id"})
})
public class Notice extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="board_id", nullable = false)
    private Board board;

    @Column(name = "external_id", nullable = false, length = 255)
    private String externalId;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name="original_url", nullable = false, length = 500)
    private String originalUrl;

    @Column(name = "posted_at")
    private LocalDate postedAt;


    @Builder
    public Notice(Board board, String externalId, String title, String originalUrl, LocalDate postedAt) {
        this.board = board;
        this.externalId = externalId;
        this.title = title;
        this.originalUrl = originalUrl;
        this.postedAt = postedAt;
    }
}
