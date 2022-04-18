package com.botdiril.sql.test;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.sql.test.schema.SchemaBotdirilData;

public class TestColumnData
{
    public static void main(String[] args)
    {
        var cfg = new SqlConnectionConfig(System.getenv("DB_HOST"), "root", "changeit", "test");

        try (var modelManager = SqlEngine.create(cfg, SchemaBotdirilData.class))
        {
            var cm = modelManager.getConnectionManager();

            try (var db = cm.getReadOnly())
            {
                var values = db.getList("SELECT `ot_name` FROM `b50_data`.`object_types`", SchemaBotdirilData.TableObjectTypes.name);

                System.out.println("================");
                values.forEach(System.out::println);
                System.out.println("================");
            }
        }
    }
}
