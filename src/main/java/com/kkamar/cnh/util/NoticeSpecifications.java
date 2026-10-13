package com.kkamar.cnh.util;

import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.Notice;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 맞춤 피드 조건을 DB 쿼리로 표현한다.
 * 예전에는 학과 범위로 전체 목록을 읽은 뒤 자바 메모리에서 title.contains()로 걸렀는데,
 * 그러면 공지가 쌓일수록 매 요청이 전체를 읽게 되고 페이지네이션도 불가능하다.
 */
public final class NoticeSpecifications {

    /** LIKE 이스케이프 문자. 백슬래시는 DB마다 처리가 달라 피하고 '!'를 쓴다. */
    private static final char LIKE_ESCAPE = '!';

    private NoticeSpecifications() {
    }

    /** 학교 전체 게시판(department 없음) + 내 학과 게시판. 학과 미선택(null)이면 전체 게시판만. */
    public static Specification<Notice> inScope(Department department) {
        return (root, query, cb) -> {
            Path<Object> boardDepartment = root.get("board").get("department");
            if (department == null) {
                return cb.isNull(boardDepartment);
            }
            return cb.or(cb.isNull(boardDepartment), cb.equal(boardDepartment, department));
        };
    }

    /** 제목에 키워드 중 하나라도 포함. 키워드 속 %, _ 는 와일드카드가 아니라 글자 그대로 취급한다. */
    public static Specification<Notice> titleContainsAny(Collection<String> keywords) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            for (String keyword : keywords) {
                if (keyword == null || keyword.isBlank()) {
                    continue;
                }
                predicates.add(cb.like(root.<String>get("title"), "%" + escapeLike(keyword) + "%", LIKE_ESCAPE));
            }
            if (predicates.isEmpty()) {
                return cb.conjunction();
            }
            return cb.or(predicates.toArray(new Predicate[0]));
        };
    }

    static String escapeLike(String raw) {
        return raw
                .replace("!", "!!")
                .replace("%", "!%")
                .replace("_", "!_");
    }
}
