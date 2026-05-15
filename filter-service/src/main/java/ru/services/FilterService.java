package ru.services;

import ru.models.dto.Request;
import ru.models.dto.Response;

import java.util.concurrent.CompletionStage;


public interface FilterService {
    CompletionStage<Response.ImpulseFilterResponse> subscribe(int userId, Request dto);
    CompletionStage<Void> unsubscribe(int userId, Request dto);
}
