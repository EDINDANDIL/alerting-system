package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

@Json
public record AlertEvent(String message) {};
