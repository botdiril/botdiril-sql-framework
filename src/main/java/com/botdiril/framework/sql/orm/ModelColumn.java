package com.botdiril.framework.sql.orm;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.orm.column.Column;
import com.botdiril.framework.sql.orm.column.ColumnInfo;
import com.botdiril.framework.sql.orm.column.EnumColumnFlag;

public class ModelColumn<T>
{
    private final String name;
    private final Column columnMeta;
    private final ColumnInfo<T> info;

    private final EnumSet<EnumColumnFlag> flags;

    private final Set<ModelForeignKey> foreignKeys;

    private final ModelTable<?> table;

    ModelColumn(String name, Column columnMeta, ColumnInfo<T> info, ModelTable<?> table, EnumSet<EnumColumnFlag> flags)
    {
        if (!SqlEngine.isValidObjectName(name))
            throw new IllegalArgumentException("Invalid column name: " + name);

        this.name = name;
        this.columnMeta = columnMeta;
        this.info = info;
        this.foreignKeys = new HashSet<>();

        this.table = table;

        if (flags.contains(EnumColumnFlag.AUTO_INCREMENT))
            flags.add(EnumColumnFlag.NOT_NULL);

        this.flags = flags;
    }

    public Set<ModelForeignKey> getReferredTables()
    {
        return this.foreignKeys;
    }

    public String getName()
    {
        return this.name;
    }

    public Column getColumnMeta()
    {
        return this.columnMeta;
    }

    public ColumnInfo<T> getInfo()
    {
        return this.info;
    }

    public ModelTable<?> getTable()
    {
        return this.table;
    }

    public boolean getFlag(EnumColumnFlag flag)
    {
        return this.flags.contains(flag);
    }

    public boolean isAutoIncrement()
    {
        return this.getFlag(EnumColumnFlag.AUTO_INCREMENT);
    }

    public boolean isNullable()
    {
        return !this.getFlag(EnumColumnFlag.NOT_NULL);
    }

    public boolean isPrimaryKey()
    {
        return this.getFlag(EnumColumnFlag.PRIMARY_KEY);
    }

    public boolean isUnique()
    {
        return this.getFlag(EnumColumnFlag.UNIQUE);
    }

    @Override
    public String toString()
    {
        return this.getCreateInfo();
    }

    public String getCreateInfo()
    {
        var flagsStr = this.flags.stream()
                                 .map(EnumColumnFlag::getCreateInfo)
                                 .collect(Collectors.joining(" "));

        return this.info.toString() + " " + flagsStr;
    }
}
