package com.kkamar.cnh;

import com.kkamar.cnh.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;

/**
 * 스프링 컨텍스트가 (인메모리 H2 + 가짜 수집기/알림 채널로) 정상 기동하는지만 확인한다.
 * 다른 통합 테스트와 같은 설정을 공유하도록 IntegrationTestBase를 상속한다.
 */
class CampusNoticeHubApplicationTests extends IntegrationTestBase {

    @Test
    void contextLoads() {
    }

}
