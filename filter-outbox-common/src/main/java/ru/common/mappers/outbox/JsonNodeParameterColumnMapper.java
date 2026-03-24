package ru.common.mappers.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Nullable;
import org.postgresql.util.PGobject;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.database.jdbc.mapper.parameter.JdbcParameterColumnMapper;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;

@Component
public final class JsonNodeParameterColumnMapper implements JdbcParameterColumnMapper<JsonNode> {

    private final ObjectMapper objectMapper;

    public JsonNodeParameterColumnMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void set(PreparedStatement stmt, int index, @Nullable JsonNode value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.OTHER);
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(value);
            PGobject pg = new PGobject();
            pg.setType("jsonb");
            pg.setValue(json);
            stmt.setObject(index, pg);
        } catch (Exception e) {
            throw new SQLException("Failed to write JsonNode to jsonb", e);
        }
    }
}