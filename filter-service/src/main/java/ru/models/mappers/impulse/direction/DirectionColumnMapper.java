package ru.models.mappers.impulse.direction;

import java.sql.ResultSet;
import java.sql.SQLException;
import ru.tinkoff.kora.database.jdbc.mapper.result.JdbcResultColumnMapper;
import ru.common.util.Direction;
import ru.tinkoff.kora.common.Component;
@Component
public final class DirectionColumnMapper implements JdbcResultColumnMapper<Direction> {
    @Override
    public Direction apply(ResultSet row, int index) throws SQLException {
        short code = row.getShort(index);
        if (row.wasNull()) {
            return null;
        }
        return Direction.fromCode(code);
    }
}

