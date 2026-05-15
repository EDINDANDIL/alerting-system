package ru.controller;

import ru.models.dto.Request;
import ru.models.dto.Response;
import ru.models.exceptions.FilterNotFoundException;
import ru.models.exceptions.UserNotFoundException;
import ru.services.FilterService;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.HttpResponseEntity;
import ru.tinkoff.kora.http.common.annotation.Header;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.util.FilterServiceRegistry;

import java.util.concurrent.CompletionStage;

@Component
@HttpController
public class FilterController {

    private final FilterServiceRegistry filterServiceRegistry;
    public FilterController(FilterServiceRegistry filterServiceRegistry) {this.filterServiceRegistry = filterServiceRegistry;}

    @Json
    @HttpRoute(method = HttpMethod.POST, path = "/api/filters")
    public CompletionStage<HttpResponseEntity<Response.ImpulseFilterResponse>> subscribe(@Json Request dto, @Header("X-user-id") int userId) {

        FilterService service = filterServiceRegistry.getService(dto);

        return service.subscribe(userId, dto)
                .thenApply(response -> HttpResponseEntity.of(201, response));
    }

    @HttpRoute(method = HttpMethod.DELETE, path = "/api/filters")
    public CompletionStage<HttpServerResponse> unsubscribe(@Json Request dto, @Header("X-user-id") int userId)
            throws FilterNotFoundException, UserNotFoundException {

        FilterService service = filterServiceRegistry.getService(dto);
        return service.unsubscribe(userId, dto).thenApply(v -> HttpServerResponse.of(204));
    }
}
