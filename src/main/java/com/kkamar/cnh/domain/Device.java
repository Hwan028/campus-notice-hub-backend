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
@Table(
        name = "device",
        uniqueConstraints = @UniqueConstraint(columnNames = "device_uuid")
)
public class Device extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "device_uuid", nullable = false, length = 64)
    private String deviceUuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id") // nullable: 학과를 아직 선택 안 했을 수도 있음
    private Department department;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken; // 아직 푸시 미구현이라 당장은 null 가능

    @Builder
    public Device(String deviceUuid, Department department, String fcmToken) {
        this.deviceUuid = deviceUuid;
        this.department = department;
        this.fcmToken = fcmToken;
    }

    public void changeDepartment(Department department) {
        this.department = department;
    }

    public void changeFcmToken(String fcmToken) {
        this.fcmToken = fcmToken;
    }
}