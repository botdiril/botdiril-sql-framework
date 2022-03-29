package com.botdiril.framework.sql.orm;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.connection.WriteDBConnection;
import com.botdiril.framework.sql.orm.schema.Schema;

public class Model
{
    private final String name;
    private final Schema schemaMeta;
    private final Class<?> schemaClass;

    private final Map<String, ModelTable<?>> tables;

    private final ModelManager manager;

    public Model(String name, Schema schemaMeta, Class<?> schemaClass, ModelManager manager)
    {
        this.name = name;
        this.schemaMeta = schemaMeta;
        this.schemaClass = schemaClass;

        this.tables = new HashMap<>();

        this.manager = manager;
    }

    void addTable(ModelTable<?> table)
    {
        var name = table.getName();

        if (this.tables.containsKey(name))
            throw new IllegalArgumentException("One schema cannot contain two tables with the same name");

        this.tables.put(name, table);
    }

    public String getName()
    {
        return this.name;
    }

    public ModelTable<?> getTable(String name)
    {
        return this.tables.get(name);
    }

    public Schema getSchemaMeta()
    {
        return this.schemaMeta;
    }

    public Class<?> getSchemaClass()
    {
        return this.schemaClass;
    }

    public ModelManager getManager()
    {
        return this.manager;
    }

    void build(WriteDBConnection db)
    {
        db.createSchema(this.name);

        this.tables.forEach((name, table) -> table.build(db, this));
    }

    @Override
    public String toString()
    {
        var tableStr = this.tables.values()
                                  .stream()
                                  .map(ModelTable::toString)
                                  .collect(Collectors.joining("\n\n"));

        return """
        model %s {
        %s}""".formatted(this.name, tableStr.indent(2));
    }
}
