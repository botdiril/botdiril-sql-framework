package com.botdiril.sql.test;

import java.nio.file.Path;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.SqlConnectionConfig;

public class TestSchemaStruct
{
    public static void main(String[] args)
    {
        var cfg = new SqlConnectionConfig(System.getenv("DB_HOST"), "root", "changeit", "test");

        try (var modelManager = SqlEngine.create(cfg, Path.of("test-model-botdiril")))
        {
            System.out.println(modelManager);
        }
    }
}
