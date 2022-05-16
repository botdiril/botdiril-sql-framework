package com.botdiril.sql.test;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.RowData;
import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.sql.test.schema.SchemaBotdirilData;

public class TestRowRetrieval
{
    public static void main(String[] args)
    {
        var cfg = new SqlConnectionConfig(System.getenv("DB_HOST"), "root", "changeit", "test");

        try (var modelManager = SqlEngine.create(cfg, SchemaBotdirilData.class))
        {
            var cm = modelManager.getConnectionManager();

            try (var db = cm.get())
            {
                db.simpleExecute("""
                    INSERT INTO `b50_data`.`object_types`(`ot_name`)
                    VALUES ('RowTestType')
                    """);

                db.commit();
            }

            try (var db = cm.getReadOnly())
            {
                var values = db.getRow("SELECT * FROM `b50_data`.`object_types` LIMIT 1");

                System.out.println("================");

                var valStr = values.map(v -> "%s created at %s".formatted(v.getUnwrapValue(SchemaBotdirilData.TableObjectTypes.name), v.getUnwrapValue(SchemaBotdirilData.TableObjectTypes.time_created)))
                                          .orElse(null);

                System.out.println(valStr);

                System.out.println("=====COLUMNS====");

                values.map(RowData::getColumns)
                      .ifPresentOrElse(map -> map.forEach((name, type) -> System.out.printf("  %s - %s%n", name, type.getName())), () -> System.out.println("No row present."));

                System.out.println("================");
            }
        }
    }
}
