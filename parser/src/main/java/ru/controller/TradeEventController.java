package ru.controller;

import ru.service.SimulationEngine;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;

@Component
@HttpController
public class TradeEventController {

    private final SimulationEngine engine;

    public TradeEventController(SimulationEngine engine) {
        this.engine = engine;
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
        return HttpServerResponse.of(200, ru.tinkoff.kora.http.common.body.HttpBody.json(statusJson));
    }
}