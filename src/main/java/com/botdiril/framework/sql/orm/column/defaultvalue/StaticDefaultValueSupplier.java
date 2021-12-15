package com.botdiril.framework.sql.orm.column.defaultvalue;

public class StaticDefaultValueSupplier
{
    public static class ZeroInteger extends StaticValueSupplier<java.lang.Integer>
    {
        protected ZeroInteger()
        {
            super(0);
        }
    }

    public static class ZeroLong extends StaticValueSupplier<java.lang.Long>
    {
        protected ZeroLong()
        {
            super(0L);
        }
    }

    public static class ZeroFloat extends StaticValueSupplier<java.lang.Float>
    {
        protected ZeroFloat()
        {
            super(0.0f);
        }
    }

    public static class ZeroDouble extends StaticValueSupplier<java.lang.Double>
    {
        protected ZeroDouble()
        {
            super(0.0d);
        }
    }

    public static class TrueBoolean extends StaticValueSupplier<java.lang.Boolean>
    {
        protected TrueBoolean()
        {
            super(true);
        }
    }

    public static class FalseBoolean extends StaticValueSupplier<java.lang.Boolean>
    {
        protected FalseBoolean()
        {
            super(false);
        }
    }

    public static class EmptyString extends StaticValueSupplier<java.lang.String>
    {
        protected EmptyString()
        {
            super("");
        }
    }
}
