package ru.common.mappers.direction;

import com.fasterxml.jackson.core.JsonParser;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.common.util.Direction;

import java.io.IOException;

@Component
public final class DirectionJsonReader implements JsonReader<Direction> {
    @Override
    public Direction read(JsonParser parser) throws IOException {
        int code = parser.getIntValue();
        return Direction.fromCode((short) code);
    }
}