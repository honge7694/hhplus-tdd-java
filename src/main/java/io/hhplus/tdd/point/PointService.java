package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * User의 포인트를 충전한다.
     * @param userId
     * @param point
     * @return
     */
    public UserPoint chargePoint(Long userId, Long point) {
        UserPoint userPoint = userPointTable.selectById(userId);
        pointHistoryTable.insert(userPoint.id(), point, TransactionType.CHARGE, System.currentTimeMillis());
        return userPointTable.insertOrUpdate(userPoint.id(), point);
    }

    /**
     * User의 포인트를 조회한다.
     * @param userId
     * @return
     */
    public UserPoint getUserPoint(Long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * User의 포인트 내역을 조호한다.
     * @param userId
     * @return
     */
    public List<PointHistory> getUserPointHistory(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }
}
