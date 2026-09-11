package com.tju.elm_bk.utils;

import com.tju.elm_bk.exception.APIException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AiCircuitBreakerTest {
    @Test
    void shouldOpenAfterThreeConsecutiveFailures() {
        AiCircuitBreaker breaker = new AiCircuitBreaker();
        for (int i = 0; i < 3; i++) {
            assertThrows(IllegalStateException.class,
                    () -> breaker.execute("test-ai", () -> { throw new IllegalStateException("failed"); }));
        }

        assertTrue(breaker.isOpen("test-ai"));
        assertThrows(APIException.class, () -> breaker.execute("test-ai", () -> "not-called"));
    }
}
