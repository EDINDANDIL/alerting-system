package ru.controller;

import ru.domain.market.Exchange;
import ru.service.MarketDataBroadcaster;
import ru.domain.simulation.SimulationEngine;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.common.annotation.Query;
import ru.tinkoff.kora.http.common.body.HttpBody;
import ru.tinkoff.kora.http.common.body.HttpBodyOutput;
import ru.tinkoff.kora.http.common.header.HttpHeaders;
import ru.tinkoff.kora.http.common.header.MutableHttpHeaders;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;
import ru.tinkoff.kora.json.common.annotation.Json;

import java.util.List;

@Component
@HttpController
public class TradeEventController {

    private final SimulationEngine engine;
    private final Exchange exchange;
    private final MarketDataBroadcaster broadcaster;

    public TradeEventController(SimulationEngine engine, Exchange exchange, MarketDataBroadcaster broadcaster) {
        this.engine = engine;
        this.exchange = exchange;
        this.broadcaster = broadcaster;
    }

    @HttpRoute(method = HttpMethod.POST, path = "/api/simulator/start")
    public HttpServerResponse start() {
        engine.start();
        return HttpServerResponse.of(200);
    }

    @HttpRoute(method = HttpMethod.POST, path = "/api/simulator/stop")
    public HttpServerResponse stop() {
        engine.stop();
        return HttpServerResponse.of(200);
    }

    @HttpRoute(method = HttpMethod.GET, path = "/api/simulator/status")
    public HttpServerResponse getStatus() {
        String statusJson = String.format(
                "{\"running\": %b, \"currentTick\": %d}",
                engine.isRunning(),
                engine.getCurrentTick()
        );
        return HttpServerResponse.of(200, HttpBody.json(statusJson));
    }

    @Json
    @HttpRoute(method = HttpMethod.GET, path = "/api/simulator/symbols")
    public List<String> getSymbols() {
        return exchange.getSymbols();
    }

    @HttpRoute(method = HttpMethod.GET, path = "/api/simulator/stream")
    public HttpServerResponse streamMarketData(@Query("symbol") String symbol) {
        MutableHttpHeaders headers = HttpHeaders.of();
        headers.set("Cache-Control", "no-cache");
        headers.set("Connection", "keep-alive");

        return HttpServerResponse.of(
                200,
                headers,
                HttpBodyOutput.of(
                        "text/event-stream",
                        broadcaster.register(symbol)
                )
        );
    }
}