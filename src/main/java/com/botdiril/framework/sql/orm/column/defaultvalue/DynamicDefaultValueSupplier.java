package com.botdiril.framework.sql.orm.column.defaultvalue;

import java.sql.Timestamp;

public class DynamicDefaultValueSupplier
{
    public static class UTCTimestampNow extends ExpressionDefaultValueSupplier<Timestamp>
    {
        protected UTCTimestampNow()
        {
            super("UTC_TIMESTAMP");
        }
    }

    public static class TimestampNow extends ExpressionDefaultValueSupplier<Timestamp>
    {
        protected TimestampNow()
        {
            super("CURRENT_TIMESTAMP");
        }
    }
}
