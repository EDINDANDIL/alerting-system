package ru.controller;

import ru.models.dto.Request;
import ru.models.exceptions.FilterNotFoundException;
import ru.models.exceptions.UserNotFoundException;
import ru.services.FilterService;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.common.body.HttpBody;
import ru.tinkoff.kora.http.server.common.HttpServerRequest;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.util.FilterServiceRegistry;

import java.util.Objects;

@Component
@HttpController
public class FilterController {

    private final FilterServiceRegistry filterServiceRegistry;

    public FilterController(FilterServiceRegistry filterServiceRegistry) {
        this.filterServiceRegistry = filterServiceRegistry;
    }

    @HttpRoute(method = HttpMethod.POST, path = "/api/filters")
    public HttpServerResponse subscribe(@Json Request dto, HttpServerRequest request) {

        int userId = Integer.parseInt(
                Objects.requireNonNull(request.headers().getFirst("X-user-id"))
        );

        FilterService service = filterServiceRegistry.getService(dto);
        service.subscribe(userId, dto);

        return HttpServerResponse.of(200, HttpBody.plaintext("ok"));
    }


    @HttpRoute(method = HttpMethod.DELETE, path = "/api/filters")

    public HttpServerResponse unsubscribe(@Json Request dto, HttpServerRequest request)
            throws FilterNotFoundException, UserNotFoundException {

        int userId = Integer.parseInt(Objects.requireNonNull(request.headers().getFirst("X-user-id")));

        FilterService service = filterServiceRegistry.getService(dto);
        service.unsubscribe(userId, dto);

        return HttpServerResponse.of(200, HttpBody.plaintext("ok"));
    }

}
