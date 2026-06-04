package ru.controller;

import ru.services.AlertStreamService;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.common.annotation.Query;
import ru.tinkoff.kora.http.common.body.HttpBodyOutput;
import ru.tinkoff.kora.http.common.header.HttpHeaders;
import ru.tinkoff.kora.http.common.header.MutableHttpHeaders;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;

@Component
@HttpController
public final class AlertStreamController {

    private final AlertStreamService streamService;

    public AlertStreamController(AlertStreamService streamService) {
        this.streamService = streamService;
    }

    @HttpRoute(method = HttpMethod.GET, path = "/api/alerts/stream")
    public HttpServerResponse stream(@Query("userId") long userId) {
        MutableHttpHeaders headers = HttpHeaders.of();
        headers.set("Cache-Control", "no-cache");
        headers.set("Connection", "keep-alive");

        return HttpServerResponse.of(
                200,
                headers,
                HttpBodyOutput.of(
                        "text/event-stream",
                        streamService.connect(userId)
                )
        );
    }
}
