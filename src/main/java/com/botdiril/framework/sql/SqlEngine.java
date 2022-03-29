package com.botdiril.framework.sql;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Objects;

import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.framework.sql.orm.ModelManager;
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

    public static ModelManager create(SqlConnectionConfig config, Path... modelDirs)
    {
        try
        {
            var manager = new ModelManager(config);
            Arrays.stream(modelDirs).forEach(manager::registerModels);
            manager.initialize();

            return manager;
        }
        catch (Exception e)
        {
            throw new BotdirilInitializationException("An error has occured while creating the SQL connection.", e);
        }
    }

    public static boolean isValidObjectName(String name)
    {
        Objects.requireNonNull(name);

        return name.matches("[a-zA-Z]+?[a-zA-Z0-9_]*?[a-zA-Z0-9_]+");
    }
}
