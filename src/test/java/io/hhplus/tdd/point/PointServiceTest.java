package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @Test
    @DisplayName("회원의 포인트를 충전한다.")
    public void chargeUserPoint() throws Exception {
        // given
        Long userId = 1L;
        Long chargePoint = 100L;
        long fixedTime = System.currentTimeMillis();
        UserPoint user = UserPoint.empty(userId);

        when(userPointTable.selectById(userId)).thenReturn(user);
        when(pointHistoryTable.insert(userId, chargePoint, TransactionType.CHARGE, fixedTime))
                .thenReturn(new PointHistory(1, userId, chargePoint, TransactionType.CHARGE, fixedTime));
        when(userPointTable.insertOrUpdate(user.id(), chargePoint))
                .thenReturn(new UserPoint(userId, chargePoint, System.currentTimeMillis()));

        // when
        PointHistory userPointHistory = pointHistoryTable.insert(userId, chargePoint, TransactionType.CHARGE, fixedTime);
        UserPoint userPoint = pointService.chargePoint(userId, chargePoint);

        // then
        Assertions.assertThat(userPoint.point()).isEqualTo(100L);
        Assertions.assertThat(userPointHistory.amount()).isEqualTo(100L);
        verify(userPointTable).insertOrUpdate(userId, chargePoint);
        verify(pointHistoryTable).insert(userId, chargePoint, TransactionType.CHARGE, fixedTime);
    }
}