package com.botdiril.sql.test;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.framework.sql.util.SqlLogger;

public class TestBotdiril
{
    public static void main(String[] args)
    {
        try
        {
            var config = new SqlConnectionConfig("localhost:3306", "root", null, "botdiril50");

            try (var sql = SqlEngine.create(config))
            {
                try (var db = sql.getReadOnly())
                {
                    var tables = db.getList("""
                        SELECT
                            `TABLE_NAME` AS `table_name`
                        FROM information_schema.TABLES
                        WHERE
                            `TABLE_SCHEMA` = ?
                        """, "table_name", String.class, config.defaultSchema());

                    SqlLogger.instance.info("Tables:");
                    tables.forEach(SqlLogger.instance::info);
                }
            }
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
}
