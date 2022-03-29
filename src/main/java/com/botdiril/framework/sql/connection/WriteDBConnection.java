package com.botdiril.framework.sql.connection;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import com.botdiril.framework.sql.ISqlExecuteFunction;
import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.util.DBException;
import com.botdiril.framework.sql.util.SqlLogger;

public final class WriteDBConnection extends ReadDBConnection
{
    WriteDBConnection(Connection connection, boolean autocommit)
    {
        super(connection, autocommit, false);
    }

    public static WriteDBConnection fromExisting(Connection c)
    {
        try
        {
            if (c.isReadOnly())
                throw new DBException("Cannot use a read-only connection for a WriteDBConnection!");

            return new WriteDBConnection(c, c.getAutoCommit());
        }
        catch (SQLException e)
        {
            throw new DBException(e);
        }
    }

    public int simpleUpdate(@Language("MySQL") String statement, Object... params)
    {
        return this.executeStatement(statement, PreparedStatement::executeUpdate, Integer::intValue, params);
    }

    public boolean simpleExecute(@Language("MySQL") String statement, Object... params)
    {
        return this.executeStatement(statement, PreparedStatement::execute, Boolean::booleanValue, params);
    }

    public <R> R exec(@Language("MySQL") String statement, ISqlExecuteFunction<R> callback, Object... params)
    {
        return this.exec(statement, false, callback, params);
    }

    public <R> R exec(@Language("MySQL") String statement, boolean generateKeys, ISqlExecuteFunction<R> callback, Object... params)
    {
        return this.executeStatement(generateKeys, statement, callback, result -> result, params);
    }

    public boolean createSchema(String name)
    {
        if (!SqlEngine.isValidObjectName(name))
            throw new DBException(name + " is not a valid schema name!");

        if (this.schemaExists(name))
            return false;

        SqlLogger.instance.info("Creating schema `{}`.", name);

        this.simpleExecute("CREATE SCHEMA " + name + " CHARACTER SET = 'utf8'");

        return true;
    }
}
