package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.CrawlLog;
import com.kkamar.cnh.enums.CrawlHealth;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CrawlLogRepository extends JpaRepository<CrawlLog, Long> {

    @EntityGraph(attributePaths = "board")
    List<CrawlLog> findByBoardIdOrderByStartedAtDesc(Long boardId, Pageable pageable);

    @EntityGraph(attributePaths = "board")
    List<CrawlLog> findAllByOrderByStartedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = "board")
    Optional<CrawlLog> findFirstByBoardIdOrderByStartedAtDesc(Long boardId);

    Optional<CrawlLog> findFirstByBoardIdAndSuccessTrueOrderByStartedAtDesc(Long boardId);

    /**
     * 기준선 후보: "정상으로 판정됐던" 성공 사이클만 최근순으로.
     * DEGRADED/DOWN 사이클을 제외하므로, 장애가 길어져도 기준선이 고장 상태로 물들지 않는다.
     */
    @Query("select c from CrawlLog c " +
            "where c.board.id = :boardId and c.success = true and c.rowsFound >= :minRows " +
            "and c.healthStatus in :statuses and c.startedAt >= :since " +
            "order by c.startedAt desc")
    List<CrawlLog> findBaselineCandidates(@Param("boardId") Long boardId,
                                          @Param("minRows") int minRows,
                                          @Param("statuses") Collection<CrawlHealth> statuses,
                                          @Param("since") LocalDateTime since,
                                          Pageable pageable);
}
