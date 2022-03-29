package com.botdiril.framework.sql.orm;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.WriteDBConnection;
import com.botdiril.framework.sql.orm.table.Table;
import com.botdiril.framework.sql.util.SqlLogger;

public class ModelTable<T>
{
    private final String name;
    private final T table;
    private final Table tableMeta;
    private final Class<T> tableClass;

    private final Model model;

    private final Map<String, ModelColumn<?>> columns;

    ModelTable(String name, T table, Table tableMeta, Class<T> tableClass, Model model)
    {
        if (!SqlEngine.isValidObjectName(name))
            throw new IllegalArgumentException("Invalid table name.");

        this.name = name;
        this.table = table;
        this.tableMeta = tableMeta;
        this.tableClass = tableClass;
        this.model = model;
        this.columns = new HashMap<>();
    }

    void addColumn(ModelColumn<?> column)
    {
        var name = column.getName();

        if (this.columns.containsKey(name))
            throw new IllegalArgumentException("One table cannot contain two keys with the same name");

        this.columns.put(name, column);
    }

    public String getName()
    {
        return this.name;
    }

    public T getTable()
    {
        return this.table;
    }

    public Table getTableMeta()
    {
        return this.tableMeta;
    }

    public Class<T> getTableClass()
    {
        return this.tableClass;
    }

    public Model getSchema()
    {
        return this.model;
    }

    void build(WriteDBConnection db, Model model)
    {
        db.simpleExecute("USE " + model.getName());

        if (db.tableExists(this.name))
            return;

        var tblQualifiedName = model.getName() + "." + this.name;

        SqlLogger.instance.info("Creating table {}.", tblQualifiedName);

        var colCreateInfo = columns.values()
                                   .stream()
                                   .map(ModelColumn::getCreateInfo)
                                   .collect(Collectors.joining(", \n"));

        db.simpleExecute("""
        CREATE TABLE %s (
        %s
        )
        """.formatted(this.name, colCreateInfo.indent(2)));
    }

    @Override
    public String toString()
    {
        var colStr = this.columns.values()
                                 .stream()
                                 .map(ModelColumn::toString)
                                 .collect(Collectors.joining("\n"));

        return """
        table %s {
        %s}""".formatted(this.name, colStr.indent(2));
    }
}
