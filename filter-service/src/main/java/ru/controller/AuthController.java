package ru.controller;

import ru.models.dto.Request;
import ru.models.dto.Response;
import ru.persistence.entity.UserEntity;
import ru.services.UserService;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.http.common.HttpMethod;
import ru.tinkoff.kora.http.common.HttpResponseEntity;
import ru.tinkoff.kora.http.common.annotation.HttpRoute;
import ru.tinkoff.kora.http.server.common.annotation.HttpController;
import ru.tinkoff.kora.json.common.annotation.Json;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import java.util.Date;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

@Component
@HttpController
public class AuthController {

    private final UserService userService;
    public static final String JWT_SECRET = "super-secret-key-change-in-production"; 
    private static final long EXPIRATION_TIME_MS = 86_400_000;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @Json
    @HttpRoute(method = HttpMethod.POST, path = "/api/auth/register")
    public CompletionStage<HttpResponseEntity<Response.AuthResponse>> register(@Json Request.AuthRequest request) {
        return userService.register(request.email(), request.password())
        .thenApply(_ ->
                HttpResponseEntity.of(201, new Response.AuthResponse("Ok"))
        )
        .exceptionally(_ ->
                HttpResponseEntity.of(400, new Response.AuthResponse("User already exists or invalid data"))
        );
    }

    @Json
    @HttpRoute(method = HttpMethod.POST, path = "/api/auth/login")
    public CompletionStage<HttpResponseEntity<Response.AuthResponse>> login(@Json Request.AuthRequest request) {
        return userService.findByEmail(request.email())
            .thenCompose(userOpt -> {
                if (userOpt.isEmpty()) return CompletableFuture.completedStage(HttpResponseEntity.of(401, new Response.AuthResponse(null)));
                UserEntity user = userOpt.get();
            return userService.checkPassword(request.password(), user.passwordHash())
                    .thenApply(matched -> {
                        if (!matched) return HttpResponseEntity.of(401, new Response.AuthResponse("Wrong password"));
                        String token = JWT.create()
                                .withClaim("userId", user.id())
                                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME_MS))
                                .sign(Algorithm.HMAC256(JWT_SECRET));
                        return HttpResponseEntity.of(200, new Response.AuthResponse(token));
                    });
        });
    }
}
