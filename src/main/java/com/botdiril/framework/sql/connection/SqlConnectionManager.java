package com.botdiril.framework.sql.connection;

import com.mchange.v2.c3p0.ComboPooledDataSource;

import java.beans.PropertyVetoException;
import java.sql.Connection;
import java.sql.SQLException;

import com.botdiril.framework.sql.IDBResource;
import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.util.DBException;

public class SqlConnectionManager implements IDBResource
{
    private static final int IDLE_CONNECTION_TEST_PERIOD = 60;
    private static final int MAX_CONNECTION_AGE = 60 * 60;

    private final ComboPooledDataSource dataSource;

    public SqlConnectionManager(SqlConnectionConfig config) throws PropertyVetoException
    {
        var schema = config.defaultSchema();

        var url = "jdbc:mysql:// " + config.host() + "/" + schema +
                "?useUnicode=true" +
                "&autoReconnect=true" +
                "&useJDBCCompliantTimezoneShift=true" +
                "&useLegacyDatetimeCode=false" +
                "&serverTimezone=UTC";

        this.dataSource = new ComboPooledDataSource();
        this.dataSource.setDriverClass(SqlEngine.DRIVER_CLASS.getName());
        this.dataSource.setIdleConnectionTestPeriod(IDLE_CONNECTION_TEST_PERIOD);
        this.dataSource.setTestConnectionOnCheckin(true);
        this.dataSource.setMaxConnectionAge(MAX_CONNECTION_AGE);
        this.dataSource.setJdbcUrl(url);
        this.dataSource.setUser(config.username());
        this.dataSource.setPassword(config.password());
        this.dataSource.setAutoCommitOnClose(false);
    }

    @Override
    public void close() throws DBException
    {
        this.dataSource.close();
    }

    public WriteDBConnection get()
    {
        return get(false);
    }

    public WriteDBConnection get(boolean autocommit)
    {
        var c = get(autocommit, false);
        return new WriteDBConnection(c, autocommit);
    }

    public ReadDBConnection getReadOnly()
    {
        var c = get(true, true);
        return new ReadDBConnection(c, true);
    }

    private Connection get(boolean autocommit, boolean readOnly)
    {
        try
        {
            var c = this.dataSource.getConnection();
            c.setAutoCommit(autocommit);
            c.setReadOnly(readOnly);
            return c;
        }
        catch (SQLException e)
        {
            throw new DBException(e);
        }
    }
}