package com.botdiril.framework.sql.orm.column.defaultvalue;

public class DynamicDefaultValueSupplier
{
    public static class UTCTimestampNow extends ExpressionDefaultValueSupplier
    {
        public UTCTimestampNow()
        {
            super("UTC_TIMESTAMP");
        }
    }

    public static class TimestampNow extends ExpressionDefaultValueSupplier
    {
        public TimestampNow()
        {
            super("CURRENT_TIMESTAMP");
        }
    }
}
