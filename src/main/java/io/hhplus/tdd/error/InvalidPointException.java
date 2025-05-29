package io.hhplus.tdd.error;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409
public class InvalidPointException extends RuntimeException {
    public InvalidPointException(String message) {
        super(message);
    }
}