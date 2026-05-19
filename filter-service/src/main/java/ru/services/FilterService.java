package ru.services;

import ru.models.dto.Request;
import ru.models.dto.Response;

import java.util.List;
import java.util.concurrent.CompletionStage;


public interface FilterService {
    CompletionStage<Response> subscribe(long userId, Request dto);
    CompletionStage<Void> unsubscribe(long userId, long filterId);
    CompletionStage<List<Response>> findFiltersByUserId(long id);
}
