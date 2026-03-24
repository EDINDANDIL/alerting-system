package ru.common.mappers.outbox;

import java.sql.ResultSet;
import java.sql.SQLException;
import ru.tinkoff.kora.common.Component;
import ru.tinkoff.kora.database.jdbc.mapper.result.JdbcResultColumnMapper;
import ru.common.util.OutboxOperation;

@Component
public final class OutboxOperationResultColumnMapper implements JdbcResultColumnMapper<OutboxOperation> {
    @Override
    public OutboxOperation apply(ResultSet row, int index) throws SQLException {
        int code = row.getInt(index);  // SMALLINT -> getInt
        if (row.wasNull()) {
            return null;
        }
        return OutboxOperation.fromCode(code);
    }
}