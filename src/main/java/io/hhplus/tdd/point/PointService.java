package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    /**
     * User의 포인트를 충전한다.
     * @param id
     * @param point
     * @return
     */
    public UserPoint chargePoint(Long id, Long point) {
        UserPoint userPoint = userPointTable.selectById(id);
        pointHistoryTable.insert(userPoint.id(), point, TransactionType.CHARGE, System.currentTimeMillis());
        return userPointTable.insertOrUpdate(userPoint.id(), point);
    }


}
