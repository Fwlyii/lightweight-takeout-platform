package elm_bk.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import elm_bk.config.DeepSeekConfig;
import elm_bk.dto.DeepSeekRequestDTO;
import elm_bk.utils.DeepSeekApiClient;
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
        server.createContext("/warmup", exchange -> {
            exchange.sendResponseHeaders(204, -1);
            exchange.close();
        });
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
            // Initialize the HTTP runtime before measuring the retry deadline.
            config.setChatEndpoint("/warmup");
            config.setTimeoutSeconds(10);
            client.chatCompletionSync(request);
            config.setChatEndpoint("/chat/completions");
            config.setTimeoutSeconds(1);
            assertTimeoutPreemptively(Duration.ofSeconds(3),
                    () -> assertThrows(RuntimeException.class, () -> client.chatCompletionSync(request)));
            assertEquals(1, requests.get(), "总超时后不能继续发送重试请求");
        } finally {
            server.stop(0);
            executor.shutdownNow();
        }
    }
}
