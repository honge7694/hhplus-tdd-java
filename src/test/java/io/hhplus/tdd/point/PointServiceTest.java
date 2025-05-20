package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
}
