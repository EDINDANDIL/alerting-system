package ru.models.mappers;

import jakarta.annotation.Nullable;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.database.jdbc.mapper.parameter.JdbcParameterColumnMapper;

import java.sql.Array;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.List;

@Component
public final class StringListParameterColumnMapper implements JdbcParameterColumnMapper<List<String>> {

    @Override
    public void set(PreparedStatement stmt, int index, @Nullable List<String> value) throws SQLException {
        if (value == null || value.isEmpty()) {
            stmt.setNull(index, Types.ARRAY);
            return;
        }
        String[] typedArray = value.toArray(String[]::new);
        Array sqlArray = stmt.getConnection().createArrayOf("TEXT", typedArray);
        stmt.setArray(index, sqlArray);
    }
}
