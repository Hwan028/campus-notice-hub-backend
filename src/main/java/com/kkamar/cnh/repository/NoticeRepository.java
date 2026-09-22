package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    boolean existsByBoardIdAndExternalId(Long boardId, String externalId);
    List<Notice> findAllByOrderByPostedAtDesc();
}
