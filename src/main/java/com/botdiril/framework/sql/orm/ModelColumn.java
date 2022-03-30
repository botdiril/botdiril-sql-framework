package com.botdiril.framework.sql.orm;

import com.mysql.cj.util.StringUtils;

import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.SqlEngine;
import com.botdiril.framework.sql.connection.WriteDBConnection;
import com.botdiril.framework.sql.orm.column.Column;
import com.botdiril.framework.sql.orm.column.ColumnInfo;
import com.botdiril.framework.sql.orm.column.EnumColumnFlag;
import com.botdiril.framework.sql.orm.column.defaultvalue.DefaultValueSupplier;
import com.botdiril.framework.sql.orm.column.defaultvalue.ExpressionDefaultValueSupplier;
import com.botdiril.framework.sql.util.SqlLogger;

public class ModelColumn<T>
{
    private final String name;
    private final Column columnMeta;
    private final ColumnInfo<T> info;

    private final EnumSet<EnumColumnFlag> flags;

    private final Set<ModelForeignKey> foreignKeys;

    private final DefaultValueSupplier<?> defaultValueSupplier;

    private final ModelTable<?> table;

    ModelColumn(String name, Column columnMeta, ColumnInfo<T> info, ModelTable<?> table, EnumSet<EnumColumnFlag> flags, DefaultValueSupplier<?> defaultValueSupplier)
    {
        if (!SqlEngine.isValidObjectName(name))
            throw new IllegalArgumentException("Invalid column name: " + name);

        this.name = name;
        this.columnMeta = columnMeta;
        this.info = info;
        this.foreignKeys = new HashSet<>();

        this.defaultValueSupplier = defaultValueSupplier;

        this.table = table;

        if (flags.contains(EnumColumnFlag.AUTO_INCREMENT))
            flags.add(EnumColumnFlag.NOT_NULL);

        this.flags = flags;
    }

    public void addForeignKey(ModelColumn<?> refColumn, int deleteAction)
    {
        var fk = new ModelForeignKey(refColumn, deleteAction);

        this.foreignKeys.add(fk);
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

    public DefaultValueSupplier<?> getDefaultValueSupplier()
    {
        return this.defaultValueSupplier;
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
        var cInfo = new StringBuilder();

        cInfo.append(this.info.toString());
        cInfo.append(" ");

        if (this.defaultValueSupplier != null)
        {
            var defaultVal = this.defaultValueSupplier.get();

            if (this.defaultValueSupplier instanceof ExpressionDefaultValueSupplier || defaultVal instanceof Number || defaultVal instanceof Boolean)
                cInfo.append("DEFAULT %s ".formatted(this.defaultValueSupplier.get()));
            else if (defaultVal instanceof String defaultValStr)
                cInfo.append("DEFAULT '%s'".formatted(StringUtils.escapeQuote(defaultValStr, "'")));
            else
                throw new IllegalStateException("Default value supplier not supported for type: " + defaultVal.getClass());
        }

        var flagsStr = this.flags.stream()
                                 .map(EnumColumnFlag::getCreateInfo)
                                 .collect(Collectors.joining(" "));

        cInfo.append(flagsStr);

        return cInfo.toString();
    }

    public void build(WriteDBConnection db)
    {
        var schema = this.table.getSchema();
        var schemaName = schema.getName();
        var tableName = this.table.getName();

        var hasRow = db.hasRow("""
            SELECT *
            FROM `information_schema`.`COLUMNS`
            WHERE
                `TABLE_SCHEMA` = ?
                    AND
                `TABLE_NAME` = ?
                    AND
                `COLUMN_NAME` = ?
            """, schemaName, tableName, this.info.name());

        if (hasRow)
            return;

        SqlLogger.instance.info("Column `{}` in table `{}` missing, recreating.", schemaName, tableName);

        var createInfo = this.getCreateInfo();

        db.simpleExecute("ALTER TABLE `%s`.`%s` ADD COLUMN %s".formatted(schemaName, tableName, createInfo));
    }

    public void buildForeignKeys(WriteDBConnection db)
    {
        var schema = this.table.getSchema();
        var schemaName = schema.getName();
        var tableName = this.table.getName();
        var colName = this.info.name();

        for (var fk : this.foreignKeys)
        {
            var refCol = fk.column();
            var refTbl = refCol.getTable();
            var refSchema = refTbl.getSchema();

            var refSchemaName = refSchema.getName();
            var refColName = refCol.getName();
            var refTblName = refTbl.getName();

            var hasRow = db.hasRow("""
                SELECT *
                FROM `information_schema`.`KEY_COLUMN_USAGE`
                WHERE
                    `TABLE_SCHEMA` = ?
                        AND
                    `TABLE_NAME` = ?
                        AND
                    `COLUMN_NAME` = ?
                        AND
                    `REFERENCED_TABLE_SCHEMA` = ?
                        AND
                    `REFERENCED_TABLE_NAME` = ?
                        AND
                    `REFERENCED_COLUMN_NAME` = ?
                """, schemaName, tableName, colName, refSchemaName, refTblName, refColName);

            if (hasRow)
                continue;

            SqlLogger.instance.info("Missing foreign key `{}`.`{}`.`{}` -> `{}`.`{}`.`{}`, recreating.",
                schemaName, tableName, colName, refSchemaName, refTblName, refColName);

            db.simpleExecute("""
                ALTER TABLE `%s`.`%s`
                ADD FOREIGN KEY (%s) REFERENCES `%s`.`%s`(%s)
                """.formatted(schemaName, tableName, colName, refSchemaName, refTblName, refColName));
        }
    }
}
