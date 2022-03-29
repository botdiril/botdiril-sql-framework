package com.botdiril.framework.sql.util;

import com.mchange.v2.c3p0.impl.NewProxyPreparedStatement;
import com.mysql.cj.jdbc.ClientPreparedStatement;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.PreparedStatement;
import java.sql.SQLException;

public class SqlLogger
{
    public static final Logger instance;

    static
    {
        instance = LogManager.getLogger("SQL Logger");
        instance.atLevel(Level.DEBUG);
    }

    public static void logStatement(PreparedStatement stat) throws SQLException
    {
        if (stat instanceof NewProxyPreparedStatement npps)
        {
            var cps = (ClientPreparedStatement) npps.unwrap(ClientPreparedStatement.class);
            var sql = cps.asSql();
            instance.debug("Executing SQL: " + sql);
        }
    }
}
