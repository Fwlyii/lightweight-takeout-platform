package com.tju.elm_bk.utils;

import com.tju.elm_bk.exception.APIException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;

@Component
public class AiCircuitBreaker {
    private static final int FAILURE_THRESHOLD = 3;
    private static final long OPEN_MILLIS = Duration.ofSeconds(30).toMillis();
    private final ConcurrentMap<String, State> states = new ConcurrentHashMap<>();

    public <T> T execute(String service, Supplier<T> operation) {
        State state = states.computeIfAbsent(service, ignored -> new State());
        long now = System.currentTimeMillis();
        synchronized (state) {
            if (state.openUntil > now) {
                throw new APIException("智能服务连续失败，正在短暂恢复中，请稍后重试");
            }
            if (state.openUntil > 0) {
                state.openUntil = 0;
                state.failures = 0;
            }
        }
        try {
            T result = operation.get();
            synchronized (state) {
                state.failures = 0;
                state.openUntil = 0;
            }
            return result;
        } catch (RuntimeException ex) {
            synchronized (state) {
                state.failures++;
                if (state.failures >= FAILURE_THRESHOLD) {
                    state.openUntil = System.currentTimeMillis() + OPEN_MILLIS;
                }
            }
            throw ex;
        }
    }

    boolean isOpen(String service) {
        State state = states.get(service);
        return state != null && state.openUntil > System.currentTimeMillis();
    }

    private static final class State {
        private int failures;
        private long openUntil;
    }
}
