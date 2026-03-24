package ru.models.mappers.impulse.direction;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import ru.tinkoff.kora.database.jdbc.mapper.parameter.JdbcParameterColumnMapper;
import ru.common.util.Direction;
import ru.tinkoff.kora.common.Component;

@Component
public final class DirectionParameterMapper implements JdbcParameterColumnMapper<Direction> {

    @Override
    public void set(PreparedStatement stmt, int index, Direction value) throws SQLException {
        if (value == null) {
            stmt.setNull(index, Types.SMALLINT);
        } else {
            stmt.setShort(index, value.code);
        }
    }
}