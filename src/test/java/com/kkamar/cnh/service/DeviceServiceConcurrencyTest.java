package com.kkamar.cnh.service;

import com.kkamar.cnh.domain.Device;
import com.kkamar.cnh.dto.DeviceResponse;
import com.kkamar.cnh.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * "조회 후 없으면 삽입" 패턴이 동시 요청에서 깨지지 않는지 검증한다.
 * 모든 스레드를 래치로 세워 두었다가 동시에 출발시켜 경합을 최대한 유발한다.
 */
class DeviceServiceConcurrencyTest extends IntegrationTestBase {

    private static final int THREADS = 8;

    @Autowired private DeviceService deviceService;

    private <T> List<T> runConcurrently(Callable<T> task) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        try {
            CountDownLatch ready = new CountDownLatch(THREADS);
            CountDownLatch start = new CountDownLatch(1);
            List<Future<T>> futures = new ArrayList<>();
            for (int i = 0; i < THREADS; i++) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    return task.call();
                }));
            }
            ready.await();
            start.countDown();

            List<T> results = new ArrayList<>();
            for (Future<T> future : futures) {
                results.add(future.get(30, TimeUnit.SECONDS)); // 예외가 있으면 여기서 테스트가 실패한다
            }
            return results;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void concurrentRegistrationOfTheSameUuid_createsExactlyOneDevice() throws Exception {
        String uuid = "concurrent-" + UUID.randomUUID();

        List<DeviceResponse> responses = runConcurrently(() -> deviceService.registerDevice(uuid, null));

        Set<Long> ids = new HashSet<>();
        responses.forEach(r -> ids.add(r.getId()));
        assertThat(ids).hasSize(1);
        assertThat(deviceRepository.findAll()).hasSize(1);
    }

    @Test
    void concurrentSubscriptionToTheSameNewKeyword_createsOneKeywordAndOneSubscription() throws Exception {
        Device device = saveDevice("subscribe-device", null);

        runConcurrently(() -> {
            deviceService.subscribeKeyword(device.getId(), "동시키워드");
            return Boolean.TRUE;
        });

        assertThat(keywordRepository.findByName("동시키워드")).isPresent();
        assertThat(keywordRepository.count()).isEqualTo(1);
        assertThat(deviceKeywordRepository.findByDeviceId(device.getId())).hasSize(1);
    }
}
