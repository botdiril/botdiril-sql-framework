package com.botdiril.framework.sql.util;

public class DBException extends RuntimeException
{
    public DBException(Throwable cause)
    {
        super(cause);
    }

    public DBException(String message)
    {
        super(message);
    }
}
