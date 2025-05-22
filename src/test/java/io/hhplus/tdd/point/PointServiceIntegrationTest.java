package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@DisplayName("PointService 통합 테스트")
class PointServiceIntegrationTest {

    @Autowired
    private PointService pointService;

    @Autowired
    private UserPointTable userPointTable;

    @Autowired
    private PointHistoryTable pointHistoryTable;

    @BeforeEach
    void setUp() {
        userPointTable.selectById(2L);
    }

    @Test
    @DisplayName("[동시성 제어] - 포인트 충전")
    public void chargePointConcurrently() throws InterruptedException {
        int threadCount = 100;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch threadLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> { // 리턴값 없는 비동기 실행
                try {
                    startLatch.await();
                    pointService.chargePoint(2L, 100L);
                } catch (Exception e) {
                    System.out.println("error = " + e);
                    e.printStackTrace();
                } finally {
                    threadLatch.countDown();
                }
            }, executorService));
        }

        startLatch.countDown(); // 모든 스레드 시작
        threadLatch.await(); // 모두 끝날 때까지 대기

        UserPoint user = userPointTable.selectById(2L);
        Assertions.assertThat(user.point()).isEqualTo(10000L);
    }
}