package com.kkamar.cnh.domain;

import com.kkamar.cnh.component.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 디바이스가 저장(스크랩)한 공지. 같은 공지를 두 번 스크랩할 수 없도록 (device_id, notice_id)에 유니크 제약을 건다.
 * colorIndex는 앱 캘린더/목록에서 스크랩을 색으로 구분하기 위한 값(0 ~ 7).
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "scrap",
        uniqueConstraints = @UniqueConstraint(name = "uk_scrap_device_notice", columnNames = {"device_id", "notice_id"})
)
public class Scrap extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "device_id", nullable = false)
    private Device device;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notice_id", nullable = false)
    private Notice notice;

    @Column(name = "color_index", nullable = false)
    private int colorIndex;

    @Builder
    public Scrap(Device device, Notice notice, int colorIndex) {
        this.device = device;
        this.notice = notice;
        this.colorIndex = colorIndex;
    }
}
