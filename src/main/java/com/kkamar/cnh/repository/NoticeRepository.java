package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.Notice;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NoticeRepository extends JpaRepository<Notice, Long> {
    boolean existsByBoardIdAndExternalId(Long boardId, String externalId);
    List<Notice> findAllByOrderByPostedAtDesc();

    // 학과 스코프 필터: 전체공지(department가 null인 게시판) + 해당 학과 게시판
    List<Notice> findByBoard_DepartmentIsNullOrBoard_DepartmentOrderByPostedAtDesc(Department department);
}
