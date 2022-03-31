package com.botdiril.framework.sql.orm;

import com.botdiril.framework.sql.orm.column.ForeignKey;

public record ModelForeignKey(ModelColumn<?> column,
                              ForeignKey.ParentDeleteAction parentDeleteAction)
{
}
