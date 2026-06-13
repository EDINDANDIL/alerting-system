package ru.controller;

import ru.dto.Message;
import ru.models.Simulation;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;
import ru.tinkoff.kora.json.common.annotation.Json;

@Component
@HttpController
public class TradeEventController {

    private final Simulation generator;

    public TradeEventController(Simulation generator) {
        this.generator = generator;
    }

    @HttpRoute(method = HttpMethod.POST, path = "/api/trades/generate")
    public HttpServerResponse generate() throws InterruptedException {

        generator.runTickParallel();

        return HttpServerResponse.of(200);
    }
}
