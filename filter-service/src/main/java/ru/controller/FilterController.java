package ru.controller;

import ru.controller.handler.AuthInterceptor;
import ru.models.dto.FilterType;
import ru.models.dto.Request;
import ru.models.dto.Response;
import ru.models.exceptions.UserNotFoundException;
import ru.services.FilterService;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Context;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.HttpResponseEntity;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.common.annotation.Path;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;
import ru.tinkoff.kora.json.common.annotation.Json;
import ru.util.FilterServiceRegistry;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@HttpController
public class FilterController {

    private final FilterServiceRegistry filterServiceRegistry;

    public FilterController(FilterServiceRegistry filterServiceRegistry) {
        this.filterServiceRegistry = filterServiceRegistry;
    }

    @Json
    @HttpRoute(method = HttpMethod.POST, path = "/api/filters/{type}")
    public CompletionStage<HttpResponseEntity<Response>> subscribe(
            @Json Request dto,
            @Path FilterType type
    ) {
        Long userId = Context.current().get(AuthInterceptor.USER_ID_KEY);

        if (userId == null) throw new UserNotFoundException("User not found");

        FilterService service = filterServiceRegistry.getService(type);

        return service.subscribe(userId, dto)
                .thenApply(response -> HttpResponseEntity.of(201, response));
    }

    @HttpRoute(method = HttpMethod.DELETE, path = "/api/filters/{type}/{id}")
    public CompletionStage<HttpServerResponse> unsubscribe(
            @Path FilterType type,
            @Path long id
    ) {
        Long userId = Context.current().get(AuthInterceptor.USER_ID_KEY);

        if (userId == null) throw new UserNotFoundException("User not found");

        FilterService service = filterServiceRegistry.getService(type);

        return service.unsubscribe(userId, id)
                .thenApply(ignored -> HttpServerResponse.of(204));
    }

    @Json
    @HttpRoute(method = HttpMethod.GET, path = "/api/filters")
    public CompletionStage<HttpResponseEntity<List<Response>>> getAllFilters() {
        Long userId = Context.current().get(AuthInterceptor.USER_ID_KEY);

        if (userId == null) throw new UserNotFoundException("User not found");

        List<CompletableFuture<List<Response>>> futures = filterServiceRegistry.allFilterServices()
                .stream()
                .map(service -> service.findFiltersByUserId(userId).toCompletableFuture())
                .toList();

        return CompletableFuture.allOf(futures.toArray(CompletableFuture[]::new))
                .thenApply(ignored -> futures.stream()
                        .flatMap(future -> future.join().stream())
                        .toList())
                .thenApply(filters -> HttpResponseEntity.of(200, filters));
    }
}