package com.botdiril.framework.sql;

import java.sql.PreparedStatement;
import java.sql.SQLException;

@FunctionalInterface
public interface ISqlExecuteFunction<R>
{
    R apply(PreparedStatement preparedStatement) throws SQLException;
}
