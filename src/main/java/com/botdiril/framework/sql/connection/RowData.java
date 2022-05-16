package com.botdiril.framework.sql.connection;

import java.util.*;

import com.botdiril.framework.sql.orm.ModelColumn;

public class RowData
{
    private final Map<String, Optional<?>> values;
    private final Map<String, Class<?>> columns;

    RowData()
    {
        this.values = new HashMap<>();
        this.columns = new LinkedHashMap<>();
    }

    public Map<String, Class<?>> getColumns()
    {
        return Collections.unmodifiableMap(this.columns);
    }

    <T> void add(String colName, Class<? super T> rootClass, Optional<? super T> value)
    {
        this.values.put(colName, value);
        this.columns.put(colName, rootClass);
    }

    public <T> Optional<T> getValue(String value, Class<T> valueType)
    {
        return values.get(value)
                     .map(valueType::cast);
    }

    public <T> Optional<T> getValue(ModelColumn<T> column)
    {
        return this.getValue(column.getName(), column.getInfo().javaType());
    }

    public <T> Optional<T> getValue(String differentName, ModelColumn<T> column)
    {
        return this.getValue(differentName, column.getInfo().javaType());
    }

    public <T> T getUnwrapValue(ModelColumn<T> column)
    {
        return this.getValue(column.getName(), column.getInfo().javaType()).orElseThrow();
    }


    @Override
    public String toString()
    {
        return this.values.toString();
    }
}
