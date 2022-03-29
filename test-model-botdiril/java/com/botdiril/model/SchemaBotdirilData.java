package com.botdiril.model;

import com.botdiril.framework.sql.orm.ModelColumn;
import com.botdiril.framework.sql.orm.column.*;
import com.botdiril.framework.sql.orm.schema.Schema;
import com.botdiril.framework.sql.orm.table.Table;

@Schema(name = "b50_data")
public class SchemaBotdirilData
{
    @Table(name = "object_names", prefix = "on")
    public static class TableObjectNames
    {
        @Column(dataType = long.class)
        @PrimaryKey
        @AutoIncrement
        public ModelColumn<Long> id;

        @Column(dataType = long.class)
        @ForeignKey(value = TableObjectTypes.class, parentDeleteAction = ForeignKey.ParentDeleteAction.CASCADE_DELETE)
        @NotNull
        public ModelColumn<TableObjectTypes> ot_id;

        @Column(dataType = String.class, bounds = 64)
        @NotNull
        public ModelColumn<String> name;
    }

    @Table(name = "items", prefix = "it")
    public static class TableItems
    {
        @Column(dataType = long.class)
        @PrimaryKey
        @AutoIncrement
        public ModelColumn<Long> id;

        @Column(dataType = long.class)
        @ForeignKey(value = TableObjectNames.class, parentDeleteAction = ForeignKey.ParentDeleteAction.CASCADE_DELETE)
        @NotNull
        public ModelColumn<Long> on_id;
    }

    @Table(name = "object_types", prefix = "ot")
    public static class TableObjectTypes
    {
        @Column(dataType = long.class)
        @PrimaryKey
        @AutoIncrement
        public ModelColumn<Long> id;

        @Column(dataType = String.class, bounds = 64)
        @NotNull
        public ModelColumn<String> name;
    }
}
