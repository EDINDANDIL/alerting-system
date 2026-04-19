package ru.controller;

import ru.dto.Message;
import ru.service.TradeEventGenerator;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;
import ru.tinkoff.kora.json.common.annotation.Json;

@Component
@HttpController
public class TradeEventController {

    private final TradeEventGenerator generator;

    public TradeEventController(TradeEventGenerator generator) {
        this.generator = generator;
    }

    @HttpRoute(method = HttpMethod.POST, path = "/api/trades/generate")
    public HttpServerResponse generate(@Json Message msg) {

        generator.generate(msg);

        return HttpServerResponse.of(200);
    }
}
