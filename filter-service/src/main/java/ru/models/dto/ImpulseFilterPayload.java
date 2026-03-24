package ru.models.dto;

import ru.tinkoff.kora.json.common.annotation.JsonWriter;
import ru.common.util.Direction;

import java.util.List;

@JsonWriter
public record ImpulseFilterPayload(
        List<String> exchange,
        List<String> market,
        List<String> blackList,
        int timeWindow,
        Direction direction,
        int percent,
        int volume24h
) {}