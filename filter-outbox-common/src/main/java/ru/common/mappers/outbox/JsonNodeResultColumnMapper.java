package ru.common.mappers.outbox;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.database.jdbc.mapper.result.JdbcResultColumnMapper;

import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public final class JsonNodeResultColumnMapper implements JdbcResultColumnMapper<JsonNode> {

    private final ObjectMapper objectMapper;

    public JsonNodeResultColumnMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public JsonNode apply(ResultSet row, int index) throws SQLException {
        Object obj = row.getObject(index);
        if (obj == null || row.wasNull()) {
            return null;
        }
        try {
            String json;
            if (obj instanceof PGobject pg) {
                json = pg.getValue();
            } else if (obj instanceof String s) {
                json = s;
            } else {
                json = objectMapper.writeValueAsString(obj);
            }
            return (json == null || json.isBlank()) ? null : objectMapper.readTree(json);
        } catch (Exception e) {
            throw new SQLException("Failed to read JsonNode from jsonb", e);
        }
    }
}