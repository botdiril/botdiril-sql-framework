package com.botdiril.framework.sql.orm.column.defaultvalue;

public abstract class StaticValueSupplier<T> extends DefaultValueSupplier<T>
{
    private final T value;

    protected StaticValueSupplier(T value)
    {
        this.value = value;
    }

    @Override
    public final T get()
    {
        return this.value;
    }
}
