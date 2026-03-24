package ru.models.mappers;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;


import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.database.jdbc.mapper.result.JdbcResultColumnMapper;

@Component
public final class StringListResultColumnMapper implements JdbcResultColumnMapper<List<String>> {

    @Override
    public List<String> apply(ResultSet row, int index) throws SQLException {
        Array array = row.getArray(index);
        if (array == null) {
            return List.of();
        }
        String[] values = (String[]) array.getArray();
        return List.of(values);
    }
}