package com.botdiril.framework.sql;

import java.sql.DriverManager;
import java.util.Objects;

import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.framework.sql.connection.SqlConnectionManager;
import com.botdiril.framework.sql.connection.WriteDBConnection;
import com.botdiril.framework.sql.util.DBException;
import com.botdiril.framework.sql.util.SqlLogger;
import com.botdiril.framework.util.BotdirilInitializationException;

public class SqlEngine
{
    public static Class<?> DRIVER_CLASS;

    static
    {
        try
        {
            DRIVER_CLASS = Class.forName(com.mysql.cj.jdbc.Driver.class.getName());
        }
            catch (ClassNotFoundException e)
        {
            throw new BotdirilInitializationException(e);
        }
    }

    public static SqlConnectionManager create(SqlConnectionConfig config)
    {
        try
        {
            var schemaName = config.defaultSchema();

            if (!isValidObjectName(schemaName))
                throw new DBException(schemaName + " is not a valid schema name!");

            var jdbcURL = "jdbc:mysql://" + config.host()
                          + "/?useUnicode=true"
                          + "&autoReconnect=true"
                          + "&useJDBCCompliantTimezoneShift=true"
                          + "&useLegacyDatetimeCode=false"
                          + "&serverTimezone=UTC";

            try (var c = DriverManager.getConnection(jdbcURL, config.username(), config.password()))
            {
                try (var db = WriteDBConnection.fromExisting(c))
                {
                    if (!db.schemaExists(schemaName))
                    {
                        SqlLogger.instance.info("Database needs to be reconstructed.");

                        db.simpleExecute("CREATE SCHEMA " + schemaName + " CHARACTER SET = 'utf8'");
                    }

                    db.commit();
                }
            }

            return new SqlConnectionManager(config);
        }
        catch (Exception e)
        {
            throw new BotdirilInitializationException("An error has occured while creating the SQL connection.", e);
        }
    }

    public static boolean isValidObjectName(String name)
    {
        Objects.requireNonNull(name);

        return name.matches("[a-zA-Z]+?[a-zA-Z0-9_]*?[a-zA-Z]+");
    }
}
