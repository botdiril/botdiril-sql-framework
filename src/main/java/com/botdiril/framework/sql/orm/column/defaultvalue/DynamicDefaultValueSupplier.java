package com.botdiril.framework.sql.orm.column.defaultvalue;

import java.sql.Date;

public class DynamicDefaultValueSupplier
{
    public static class UTCDateNow extends ExpressionDefaultValueSupplier<Date>
    {
        protected UTCDateNow()
        {
            super("UTC_TIMESTAMP");
        }
    }

    public static class DateNow extends ExpressionDefaultValueSupplier<Date>
    {
        protected DateNow()
        {
            super("CURRENT_TIMESTAMP");
        }
    }
}
