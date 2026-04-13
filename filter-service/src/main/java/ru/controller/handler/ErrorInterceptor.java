package ru.controller.handler;

import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Context;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.http.common.body.HttpBody;
import ru.tinkoff.kora.http.server.common.*;

import java.util.concurrent.CompletionException;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeoutException;

@Tag(HttpServerModule.class)
@Component
public final class ErrorInterceptor implements HttpServerInterceptor {

    @Override
    public CompletionStage<HttpServerResponse> intercept(Context context,
                                                         HttpServerRequest request,
                                                         InterceptChain chain) throws Exception {
        return chain.process(context, request).exceptionally(e -> {
            if(e instanceof CompletionException) {
                e = e.getCause();
            }
            if (e instanceof HttpServerResponseException ex) {
                return ex;
            }

            var body = HttpBody.plaintext(e.getMessage());

            return switch (e) {
                case IllegalArgumentException illegalArgumentException -> HttpServerResponse.of(400, body);
                case TimeoutException timeoutException -> HttpServerResponse.of(408, body);
                default -> HttpServerResponse.of(500, body);
            };
        });
    }
}