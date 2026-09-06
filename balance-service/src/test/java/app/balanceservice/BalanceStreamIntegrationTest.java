package app.balanceservice;

import app.balanceservice.dto.BalanceDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class BalanceStreamIntegrationTest {

    @LocalServerPort
    private int port;

    private WebTestClient webTestClient;

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + port)
                .responseTimeout(Duration.ofSeconds(15))
                .build();
    }

    @Test
    void streamReturnsTextEventStreamContentType() {
        webTestClient.get()
                .uri("/balance/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM);
    }

    @Test
    void streamDeliversAtLeastSixEventsWithValidPayloads() {
        var events = webTestClient.get()
                .uri("/balance/stream")
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .returnResult(BalanceDto.class)
                .getResponseBody();

        StepVerifier.create(events.take(6))
                .thenConsumeWhile(dto -> {
                    assertThat(dto.accountId()).isNotNull();
                    assertThat(dto.balance()).isGreaterThanOrEqualTo(0.0);
                    return true;
                })
                .verifyComplete();
    }
}
