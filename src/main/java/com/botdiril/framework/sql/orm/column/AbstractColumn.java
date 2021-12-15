package com.botdiril.framework.sql.orm.column;

import com.botdiril.framework.sql.orm.table.Table;

public class AbstractColumn<T>
{
    private String name;

    private ColumnInfo<T> info;

    private Table table;
}
