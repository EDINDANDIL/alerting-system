package ru.common.mappers.direction;

import com.fasterxml.jackson.core.JsonGenerator;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.json.common.JsonWriter;
import ru.common.util.Direction;

import java.io.IOException;

@Component
public final class DirectionJsonWriter implements JsonWriter<Direction> {
    @Override
    public void write(JsonGenerator gen, Direction value) throws IOException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value.code);
        }
    }
}
