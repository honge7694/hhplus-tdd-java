package io.hhplus.tdd.point;

import io.hhplus.tdd.database.PointHistoryTable;
import io.hhplus.tdd.database.UserPointTable;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class PointService {

    private final UserPointTable userPointTable;
    private final PointHistoryTable pointHistoryTable;

    private final Map<Long, Object> userLocks = new ConcurrentHashMap<>();

    /**
     * User의 포인트를 조회한다.
     * @param userId
     * @return
     */
    public UserPoint getUserPoint(Long userId) {
        return userPointTable.selectById(userId);
    }

    /**
     * User의 포인트 내역을 조회한다.
     * @param userId
     * @return
     */
    public List<PointHistory> getUserPointHistory(Long userId) {
        return pointHistoryTable.selectAllByUserId(userId);
    }

    /**
     * User의 포인트를 충전한다.
     * @param userId
     * @param amountPoint
     * @return
     */
    public UserPoint chargePoint(Long userId, Long amountPoint) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object()); // computeIfAbsent : thread-safe한 actomic메서드

        synchronized (lock) {
            UserPoint user = userPointTable.selectById(userId);
            UserPoint updated = user.isValidChargePoint(amountPoint);
            pointHistoryTable.insert(user.id(), amountPoint, TransactionType.CHARGE, System.currentTimeMillis());
            return userPointTable.insertOrUpdate(user.id(), updated.point());
        }
    }

    /**
     * 포인트를 사용한다.
     * @param userId
     * @param amountPoint
     * @return
     */
    public UserPoint useUserPoint(Long userId, long amountPoint) {
        Object lock = userLocks.computeIfAbsent(userId, k -> new Object());

        synchronized (lock) {
            UserPoint user = userPointTable.selectById(userId);
            UserPoint updated = user.isValidUsePoint(amountPoint);
            pointHistoryTable.insert(user.id(), amountPoint, TransactionType.USE, System.currentTimeMillis());
            return userPointTable.insertOrUpdate(user.id(), updated.point());
        }
    }
}
