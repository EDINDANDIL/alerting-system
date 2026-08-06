package ru.services;

import ru.models.dto.Request;
import ru.models.dto.Response;

import java.util.List;

public interface FilterService {
    Response subscribe(long userId, Request dto);
    void unsubscribe(long userId, long filterId);
    List<Response> findFiltersByUserId(long id);
}