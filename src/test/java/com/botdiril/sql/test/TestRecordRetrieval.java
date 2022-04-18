package com.botdiril.sql.test;

import java.time.LocalDateTime;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.sql.test.schema.SchemaBotdirilData;

public class TestRecordRetrieval
{
    public record ObjectTypes(Long ot_id, String ot_name, LocalDateTime ot_time_created) { }

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
                    VALUES ('TestType')
                    """);

                db.commit();
            }

            try (var db = cm.getReadOnly())
            {
                var values = db.getRecordList("SELECT * FROM `b50_data`.`object_types`", ObjectTypes.class);

                System.out.println("================");
                values.forEach(System.out::println);
                System.out.println("================");
            }
        }
    }
}
