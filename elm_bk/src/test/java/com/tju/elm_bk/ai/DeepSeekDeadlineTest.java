package com.tju.elm_bk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import com.tju.elm_bk.config.DeepSeekConfig;
import com.tju.elm_bk.dto.DeepSeekRequestDTO;
import com.tju.elm_bk.utils.DeepSeekApiClient;
import org.junit.jupiter.api.Test;
import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import static org.junit.jupiter.api.Assertions.*;

class DeepSeekDeadlineTest {
    @Test void retryBudgetCannotExtendTheOverallDeadline() throws Exception {
        var requests = new AtomicInteger();
        var executor = Executors.newSingleThreadExecutor();
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/chat/completions", exchange -> {
            requests.incrementAndGet();
            try {
                Thread.sleep(3000);
                exchange.sendResponseHeaders(503, -1);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            } finally {
                exchange.close();
            }
        });
        server.start();
        try {
            var config = new DeepSeekConfig();
            config.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
            config.setApiKey("local-test-only");
            config.setTimeoutSeconds(1);
            config.setMaxRetries(3);
            var request = new DeepSeekRequestDTO();
            request.setMessages(List.of(new DeepSeekRequestDTO.MessageDTO("user", "test")));
            var client = new DeepSeekApiClient(config, new ObjectMapper());
            assertTimeoutPreemptively(Duration.ofSeconds(3),
                    () -> assertThrows(RuntimeException.class, () -> client.chatCompletionSync(request)));
            assertEquals(1, requests.get(), "总超时后不能继续发送重试请求");
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
