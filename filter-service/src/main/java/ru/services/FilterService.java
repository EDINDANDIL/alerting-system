package ru.services;

import ru.models.dto.Request;

public interface FilterService {
    void subscribe(int userId, Request dto);
    void unsubscribe(int userId, Request dto);
}
