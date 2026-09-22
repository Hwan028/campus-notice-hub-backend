package com.kkamar.cnh.runner;

import com.kkamar.cnh.domain.Board;
import com.kkamar.cnh.domain.Department;
import com.kkamar.cnh.domain.University;
import com.kkamar.cnh.repository.BoardRepository;
import com.kkamar.cnh.repository.DepartmentRepository;
import com.kkamar.cnh.repository.UniversityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;


@Slf4j
@Component
@RequiredArgsConstructor
public class InitialDataRunner implements CommandLineRunner {
    private final UniversityRepository universityRepository;
    private final DepartmentRepository departmentRepository;
    private final BoardRepository boardRepository;

    @Override
    public void run(String... args) throws Exception {
        log.info("Initializing debug 1");
        if (universityRepository.count() > 0) return;
        log.info("Initializing debug 2");
        University inhatc = University.builder()
                .name("인하공업전문대학")
                .portalBaseUrl("https://www.inhatc.ac.kr")
                .build();
        universityRepository.save(inhatc);

        Department computerScience = Department.builder()
                .university(inhatc)
                .name("컴퓨터정보과")
                .college(null)
                .build();
        departmentRepository.save(computerScience);

        Board allNoticeBoard = Board.builder()
                .university(inhatc)
                .department(null)
                .name("전체공지")
                .crawlUrl("https://www.inhatc.ac.kr/ipsi/402/subview.do")
                .build();
        boardRepository.save(allNoticeBoard);

        Board deptNoticeBoard = Board.builder()
                .university(inhatc)
                .department(computerScience)
                .name("컴퓨터정보과공지")
                .crawlUrl("https://cs.inhatc.ac.kr/cs/1754/subview.do")
                .build();
        boardRepository.save(deptNoticeBoard);

        System.out.println("[InitialDataRunner] 초기 데이터 시딩 완료\"");
    }

}
