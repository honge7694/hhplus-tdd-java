package io.hhplus.tdd.point;

import io.hhplus.tdd.error.InvalidPointException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public UserPoint {
        if (point < 0 || point > 100000) {
            throw new InvalidPointException("포인트는 0 이상 100000 이하이어야 합니다.");
        }
    }

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
