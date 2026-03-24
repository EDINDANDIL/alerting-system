package ru.common.util;

/**
 * Определяет имя топика Kafka по полю action из outbox.
 */
public final class OutboxTopicResolver {

    private OutboxTopicResolver() {
    }

    /**
     * Возвращает имя топика для данного action.
     * Например: "IMPULSE" -> "Trades".
     */
    public static String topicForAction(String action) {
        if (action == null || action.isBlank()) {
            throw new IllegalArgumentException("action must not be null or blank");
        }
        return switch (action) {
            case "IMPULSE" -> "Trades";
            // case "VOLUME" -> "VolumeTopic";
            default -> throw new IllegalArgumentException("Unknown action for topic: " + action);
        };
    }
}