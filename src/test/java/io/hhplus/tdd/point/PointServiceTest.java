package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import io.hhplus.tdd.error.InvalidPointException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class PointServiceTest {

    @InjectMocks
    private PointService pointService;
    @Mock
    private UserPointTable userPointTable;
    @Mock
    private PointHistoryTable pointHistoryTable;

    private Long userId;
    private Long chargePoint;
    private long fixedTime;

    @BeforeEach
    void setUp() {
        // 반복되는 초기화 코드를 줄이기 위해 @BeforeEach 사용
        userId = 1L;
        chargePoint = 100L;
        fixedTime = System.currentTimeMillis();
        userPointTable.insertOrUpdate(2L, 0L);
    }

    @Test
    @DisplayName("[포인트 충전] - 포인트를 충전한다.")
    public void chargeUserPoint() throws Exception {
        // given
        UserPoint user = UserPoint.empty(userId);

        when(userPointTable.selectById(userId)).thenReturn(user);
        when(userPointTable.insertOrUpdate(user.id(), chargePoint))
                .thenReturn(new UserPoint(userId, chargePoint, System.currentTimeMillis()));

        // when
        UserPoint userPoint = pointService.chargePoint(userId, chargePoint);

        // then
        Assertions.assertThat(userPoint.point()).isEqualTo(100L);
        verify(userPointTable).insertOrUpdate(userId, chargePoint);
    }

    @Test
    @DisplayName("[포인트 충전] - 포인트 히스토리를 저장한다.")
    public void chargeUserPointHistory() {
        // given
        UserPoint user = UserPoint.empty(userId);

        when(userPointTable.selectById(userId)).thenReturn(user);
        when(pointHistoryTable.insert(eq(userId), eq(chargePoint), eq(TransactionType.CHARGE), anyLong()))
                .thenReturn(new PointHistory(1, userId, chargePoint, TransactionType.CHARGE, fixedTime));
        when(userPointTable.insertOrUpdate(userId, chargePoint))
                .thenReturn(new UserPoint(chargePoint, chargePoint, fixedTime));

        // when
        pointService.chargePoint(userId, chargePoint);

        // then
        verify(pointHistoryTable).insert(eq(userId), eq(chargePoint), eq(TransactionType.CHARGE), anyLong());
    }

    @Test
    @DisplayName("[포인트 조회] - 포인트를 조회한다.")
    public void getUserPoint() {
        // given
        UserPoint point = new UserPoint(userId, chargePoint, fixedTime);

        when(userPointTable.selectById(userId)).thenReturn(point);

        // when
        UserPoint selectedUser = pointService.getUserPoint(userId);

        // then
        Assertions.assertThat(selectedUser.point()).isEqualTo(chargePoint);
        verify(userPointTable).selectById(userId);
    }

    @Test
    @DisplayName("[포인트 조회] - 포인트 내역을 조회한다.")
    public void getUserPointHistory() {
        // given
        PointHistory pointHistory = new PointHistory(1, userId, chargePoint, TransactionType.CHARGE, fixedTime);

        when(pointHistoryTable.selectAllByUserId(userId))
                .thenReturn(List.of(pointHistory));

        // when
        List<PointHistory> pointHistoryList = pointService.getUserPointHistory(userId);

        // then
        Assertions.assertThat(pointHistoryList.get(0).type()).isEqualTo(TransactionType.CHARGE);
        Assertions.assertThat(pointHistoryList.get(0).amount()).isEqualTo(chargePoint);
        verify(pointHistoryTable).selectAllByUserId(userId);
    }

    @Test
    @DisplayName("[포인트 사용] - 포인트를 사용한다.")
    public void useUserPoint() {
        // given
        UserPoint userPoint = new UserPoint(userId, chargePoint, fixedTime);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(userPointTable.insertOrUpdate(userId, 0L)).thenReturn(new UserPoint(userId, 0L, fixedTime));

        // when
        UserPoint usePoint = pointService.useUserPoint(userId, 100L);

        // then
        Assertions.assertThat(usePoint.point()).isEqualTo(0L);
        verify(userPointTable).insertOrUpdate(userId, 0L);
    }

    @Test
    @DisplayName("[포인트 사용] - 포인트 히스토리를 저장한다.")
    public void useUserPointHistory() {
        // given
        UserPoint userPoint = new UserPoint(userId, chargePoint, fixedTime);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);
        when(pointHistoryTable.insert(eq(userId), eq(100L), eq(TransactionType.USE), anyLong()))
                .thenReturn(new PointHistory(1, userId, 100L, TransactionType.USE, fixedTime));

        // when
        pointService.useUserPoint(userId, 100L);

        // then
        verify(pointHistoryTable).insert(eq(userId), eq(100L), eq(TransactionType.USE), anyLong());
    }

    @Test
    @DisplayName("[포인트 사용] - 포인트 사용 실패한다.")
    public void failToUseUserPoint() {
        // given
        UserPoint userPoint = new UserPoint(userId, chargePoint, fixedTime);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // when & then
        Assertions.assertThatThrownBy(() ->
            pointService.useUserPoint(userPoint.id(), 200L))
                .isInstanceOf(InvalidPointException.class)
                .hasMessage("포인트가 부족합니다.");
    }

    @Test
    @DisplayName("[포인트 충전] - 포인트 충전 실패한다.")
    public void failToChargeUserPoint() {
        // given
        UserPoint userPoint = new UserPoint(userId, 99000L, fixedTime);

        when(userPointTable.selectById(userId)).thenReturn(userPoint);

        // when & then
        Assertions.assertThatThrownBy(() ->
            pointService.chargePoint(userId, 10000L))
                .isInstanceOf(InvalidPointException.class)
                .hasMessage("최대 포인트는 100,000 입니다.");
    }

    @Test
    @DisplayName("[동시성 제어] - 포인트 충전")
    public void chargePointConcurrently() throws InterruptedException {
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch threadLatch = new CountDownLatch(threadCount);
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(CompletableFuture.runAsync(() -> { // 리턴값 없는 비동기 실행
                try {
                    startLatch.await();
                    pointService.chargePoint(2L, chargePoint);
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

        Assertions.assertThat(user.point()).isEqualTo(1000L);
    }
}
