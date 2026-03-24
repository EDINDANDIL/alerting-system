package ru.common.mappers.outbox;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.database.jdbc.mapper.parameter.JdbcParameterColumnMapper;
import ru.common.util.OutboxOperation;

@Component
public final class OutboxOperationParameterColumnMapper implements JdbcParameterColumnMapper<OutboxOperation> {
    @Override
    public void set(PreparedStatement stmt, int index, OutboxOperation value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.SMALLINT);
        } else {
            stmt.setShort(index, (short) value.getCode());
        }
    }
}