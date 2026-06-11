package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.JsonWriter;

@JsonWriter
public record ErrorResponse(String message) {}