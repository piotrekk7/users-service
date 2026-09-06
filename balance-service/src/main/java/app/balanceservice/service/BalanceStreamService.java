package app.balanceservice.service;

import app.balanceservice.dto.BalanceDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Service
public class BalanceStreamService {

    private static final String[] ACCOUNTS = {"account1", "account2", "account3"};

    public Flux<BalanceDto> mergedStream() {
        List<Flux<BalanceDto>> streams = Arrays.stream(ACCOUNTS)
                .map(this::streamFor)
                .toList();
        return Flux.merge(streams);
    }

    private Flux<BalanceDto> streamFor(String accountId) {
        return Flux.interval(Duration.ofSeconds(1))
                .map(tick -> new BalanceDto(accountId, ThreadLocalRandom.current().nextDouble(0, 10_000)));
    }
}
