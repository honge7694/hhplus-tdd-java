package io.hhplus.tdd.point;

import io.hhplus.tdd.error.InvalidPointException;

public record UserPoint(
        long id,
        long point,
        long updateMillis
) {

    public UserPoint isValidUsePoint(long amount) {
        if (point < amount) {
            throw new InvalidPointException("포인트가 부족합니다.");
        }
        return new UserPoint(id, point - amount, System.currentTimeMillis());
    }

    public UserPoint isValidChargePoint(long amount) {
        if (100000 > (point + amount)) {
            throw new InvalidPointException("최대 포인트는 100,000 입니다.");
        }
        return new UserPoint(id, point + amount, System.currentTimeMillis());
    }

    public static UserPoint empty(long id) {
        return new UserPoint(id, 0, System.currentTimeMillis());
    }
}
