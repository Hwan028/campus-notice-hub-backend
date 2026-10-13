package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.Scrap;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScrapRepository extends JpaRepository<Scrap, Long> {

    @EntityGraph(attributePaths = {"notice", "notice.board"})
    Optional<Scrap> findByDeviceIdAndNoticeId(Long deviceId, Long noticeId);

    @EntityGraph(attributePaths = {"notice", "notice.board"})
    List<Scrap> findByDeviceIdOrderByCreatedAtDescIdDesc(Long deviceId);

    long countByDeviceId(Long deviceId);

    void deleteByDeviceIdAndNoticeId(Long deviceId, Long noticeId);
}
