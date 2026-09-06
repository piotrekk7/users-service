package app.balanceservice.controller;

import app.balanceservice.dto.BalanceDto;
import app.balanceservice.service.BalanceStreamService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceStreamService balanceStreamService;

    @GetMapping(value = "/balance/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<BalanceDto>> streamBalances() {
        return balanceStreamService.mergedStream()
                .map(dto -> ServerSentEvent.<BalanceDto>builder().data(dto).build());
    }
}
