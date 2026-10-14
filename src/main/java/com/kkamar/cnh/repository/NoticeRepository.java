package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface NoticeRepository extends JpaRepository<Notice, Long>, JpaSpecificationExecutor<Notice> {

    boolean existsByBoardIdAndExternalId(Long boardId, String externalId);

    // 응답 DTO가 board 이름을 쓰므로 목록 조회 시 board를 함께 가져온다 (N+1 방지).
    // open-in-view=false 환경에서는 트랜잭션 밖 지연 로딩이 불가능하기 때문이기도 하다.
    @Override
    @EntityGraph(attributePaths = "board")
    Page<Notice> findAll(Pageable pageable);

    @Override
    @EntityGraph(attributePaths = "board")
    Page<Notice> findAll(Specification<Notice> spec, Pageable pageable);
}
