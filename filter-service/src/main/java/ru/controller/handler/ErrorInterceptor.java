package ru.controller.handler;

import ru.models.dto.ErrorResponse;
import ru.models.exceptions.FilterNotFoundException;
import ru.models.exceptions.UserNotFoundException;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Context;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.http.common.body.HttpBody;
import ru.tinkoff.kora.http.server.common.*;
import ru.tinkoff.kora.json.common.JsonWriter;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

@Tag(HttpServerModule.class)
@Component
public final class ErrorInterceptor implements HttpServerInterceptor {

    private final JsonWriter<ErrorResponse> writer;

    public ErrorInterceptor(JsonWriter<ErrorResponse> writer) {this.writer = writer;}

    @Override
    public CompletionStage<HttpServerResponse> intercept(
            Context context,
            HttpServerRequest request,
            InterceptChain chain) throws Exception {

        return chain.process(context, request).exceptionally(e -> {

            if(e instanceof CompletionException) e = e.getCause();
            if (e instanceof HttpServerResponseException ex) return ex;

            return switch (e) {
                case IllegalArgumentException ex ->
                        error(400, new ErrorResponse(ex.getMessage()));

                case TimeoutException ex ->
                        error(408, new ErrorResponse(ex.getMessage()));

                case FilterNotFoundException ex ->
                        error(404, new ErrorResponse(ex.getMessage()));

                case UserNotFoundException ex ->
                        error(404, new ErrorResponse(ex.getMessage()));

                default ->
                        error(500, new ErrorResponse("Internal server error"));
            };
        });
    }

    private HttpServerResponse error(int code, ErrorResponse errorResponse) {
        return HttpServerResponse.of(
                code,
                HttpBody.json(writer.toStringUnchecked(errorResponse))
        );
    }
}