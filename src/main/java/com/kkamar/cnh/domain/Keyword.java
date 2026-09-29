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
        name = "keyword",
        uniqueConstraints = @UniqueConstraint(columnNames = "name")
)
public class Keyword extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String name;

    @Column(name = "is_preset", nullable = false)
    private boolean isPreset;

    @Builder
    public Keyword(String name, boolean isPreset) {
        this.name = name;
        this.isPreset = isPreset;
    }
}