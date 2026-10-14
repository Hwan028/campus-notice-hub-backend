package com.kkamar.cnh.repository;

import com.kkamar.cnh.domain.Board;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface BoardRepository extends JpaRepository<Board, Long> {

    /**
     * 크롤링 대상(활성) 게시판을 연관 엔티티와 함께 한 번에 조회한다.
     * 크롤러는 더 이상 긴 트랜잭션으로 감싸지 않으므로(네트워크 대기 중 DB 커넥션 점유 방지),
     * 지연 로딩에 기대지 않고 필요한 연관을 미리 가져온다.
     */
    @Query("select b from Board b join fetch b.university left join fetch b.department where b.active = true")
    List<Board> findAllActiveWithAssociations();
}
