package com.botdiril.framework.sql.orm.column.defaultvalue;

public class StaticDefaultValueSupplier
{
    public static class ZeroInteger extends StaticValueSupplier<java.lang.Integer>
    {
        public ZeroInteger()
        {
            super(0);
        }
    }

    public static class ZeroLong extends StaticValueSupplier<java.lang.Long>
    {
        public ZeroLong()
        {
            super(0L);
        }
    }

    public static class ZeroFloat extends StaticValueSupplier<java.lang.Float>
    {
        public ZeroFloat()
        {
            super(0.0f);
        }
    }

    public static class ZeroDouble extends StaticValueSupplier<java.lang.Double>
    {
        public ZeroDouble()
        {
            super(0.0d);
        }
    }

    public static class TrueBoolean extends StaticValueSupplier<java.lang.Boolean>
    {
        public TrueBoolean()
        {
            super(true);
        }
    }

    public static class FalseBoolean extends StaticValueSupplier<java.lang.Boolean>
    {
        public FalseBoolean()
        {
            super(false);
        }
    }

    public static class EmptyString extends StaticValueSupplier<java.lang.String>
    {
        public EmptyString()
        {
            super("");
        }
    }
}
