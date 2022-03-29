package com.botdiril.sql.test;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.framework.sql.connection.SqlConnectionManager;
import com.botdiril.framework.sql.orm.ModelManager;

public class TableTest
{
    private static ModelManager mgr;
    private static SqlConnectionManager sql;

    @BeforeAll
    public static void setup()
    {
        var config = new SqlConnectionConfig(System.getenv("DB_HOST"), "root", "changeit", "junit_test");
        mgr = SqlEngine.create(config);
        sql = mgr.getConnectionManager();
    }

    @Test
    public void createTable()
    {
        try (var db = sql.get(false))
        {
            Assertions.assertFalse(db.tableExists("test1"));

            db.simpleExecute("""                
                CREATE TABLE test1 (
                  dummy_row INT NOT NULL
                )
                """);

            Assertions.assertTrue(db.tableExists("test1"));

            db.rollback();
        }
    }

    @AfterAll
    public static void tearDown()
    {
        sql.close();
        mgr.close();
    }
}
