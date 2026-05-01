package ru.dto;

import ru.tinkoff.kora.json.common.annotation.Json;

/**
 * {
 *   "count"
 *   "startPrice": 50000,
 *   "trend": "UP",
 *   "changePercent": 5,
 *   "steps": 300,
 *   "symbol": "BST"
 * }
 */
@Json
public record Message(
        Long count,
        Long startPrice,
        String trend,
        String changePercent,
        Long steps,
        String symbol
) {}
