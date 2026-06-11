package ru.controller.handler;

import ru.controller.AuthController;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.common.Context;
import ru.tinkoff.kora.common.Tag;
import ru.tinkoff.kora.http.server.common.HttpServerInterceptor;
import ru.tinkoff.kora.http.server.common.HttpServerModule;
import ru.tinkoff.kora.http.server.common.HttpServerRequest;
import ru.tinkoff.kora.http.server.common.HttpServerResponse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import static com.auth0.jwt.JWT.require;
import static com.auth0.jwt.algorithms.Algorithm.HMAC256;

@Tag(HttpServerModule.class)
@Component
public final class AuthInterceptor implements HttpServerInterceptor {

    public static final Context.Key<Long> USER_ID_KEY = new Context.KeyImmutable<>() {};

    @Override
    public CompletionStage<HttpServerResponse> intercept(
            Context context,
            HttpServerRequest request,
            InterceptChain chain) throws Exception {

        String path = request.path();
        if (path.startsWith("/api/auth/") || path.startsWith("/api/alerts/")) {
            return chain.process(context, request);
        }

        String authHeader = request.headers().getFirst("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return CompletableFuture.completedFuture(HttpServerResponse.of(401));
        }

        String token = authHeader.substring(7);

        try {
            long userId = validateTokenAndGetUserId(token);
            context.set(USER_ID_KEY, userId);
            return chain.process(context, request);
        } catch (Exception e) {
            return CompletableFuture.completedFuture(HttpServerResponse.of(401));
        }
    }

    private long validateTokenAndGetUserId(String token) {
        return require(HMAC256(AuthController.JWT_SECRET))
                .build()
                .verify(token)
                .getClaim("userId")
                .asLong();
    }
}