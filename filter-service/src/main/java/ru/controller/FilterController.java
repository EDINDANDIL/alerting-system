package ru.controller;

import ru.models.dto.FilterType;
import ru.models.dto.Request;
import ru.models.dto.Response;
import ru.services.FilterService;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.HttpResponseEntity;
import ru.tinkoff.kora.http.common.annotation.Header;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.common.annotation.Path;
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
    @HttpRoute(method = HttpMethod.POST, path = "/api/filters/{type}")
    public CompletionStage<HttpResponseEntity<Response>> subscribe(
            @Json Request dto,
            @Header("X-user-id") long userId,
            @Path FilterType type) {

        FilterService service = filterServiceRegistry.getService(type);

        return service.subscribe(userId, dto)
                .thenApply(response -> HttpResponseEntity.of(201, response));
    }

    @HttpRoute(method = HttpMethod.DELETE, path = "/api/filters/{type}/{id}")
    public CompletionStage<HttpServerResponse> unsubscribe(
            @Header("X-user-id") long userId,
            @Path FilterType type,
            @Path long id) {

        FilterService service = filterServiceRegistry.getService(type);
        return service.unsubscribe(userId, id).thenApply(v -> HttpServerResponse.of(204));
    }
}
