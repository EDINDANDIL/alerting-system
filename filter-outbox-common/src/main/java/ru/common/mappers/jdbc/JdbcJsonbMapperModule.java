package ru.common.mappers.jdbc;

import org.postgresql.util.PGobject;
import ru.tinkoff.kora.common.Module;
import ru.tinkoff.kora.database.jdbc.mapper.parameter.JdbcParameterColumnMapper;
import ru.tinkoff.kora.database.jdbc.mapper.result.JdbcResultColumnMapper;
import ru.tinkoff.kora.json.common.JsonReader;
import ru.tinkoff.kora.json.common.JsonWriter;
import ru.tinkoff.kora.json.common.annotation.Json;

import java.sql.Types;

@Module
public interface JdbcJsonbMapperModule {

    @Json
    default <T> JdbcParameterColumnMapper<T> jdbcJsonParameterColumnMapper(JsonWriter<T> writer) {
        return (stmt, index, v) -> {
            if (v != null) {
                PGobject jsonb = new PGobject();
                jsonb.setType("jsonb");
                jsonb.setValue(writer.toStringUnchecked(v));
                stmt.setObject(index, jsonb);
            } else {
                stmt.setNull(index, Types.NULL);
            }
        };
    }

    @Json
    default <T> JdbcResultColumnMapper<T> jdbcJsonResultColumnMapper(JsonReader<T> reader) {
        return (row, index) -> {
            var value = row.getString(index);
            if (value == null) {
                return null;
            }
            return reader.readUnchecked(value);
        };
    }
}