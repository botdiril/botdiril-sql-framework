package com.botdiril.framework.sql;

import com.botdiril.framework.sql.util.DBException;

public interface IDBResource extends AutoCloseable
{
    @Override
    void close() throws DBException;
}
