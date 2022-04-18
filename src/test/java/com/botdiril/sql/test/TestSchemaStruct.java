package com.botdiril.sql.test;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.sql.test.schema.SchemaBotdirilData;

public class TestSchemaStruct
{
    public static void main(String[] args)
    {
        var cfg = new SqlConnectionConfig(System.getenv("DB_HOST"), "root", "changeit", "test");

        try (var modelManager = SqlEngine.create(cfg, SchemaBotdirilData.class))
        {
            System.out.println(modelManager);
        }
    }
}
