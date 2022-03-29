package com.botdiril.framework.sql.orm;

public record ModelForeignKey(ModelTable<?> table, ModelColumn<?> column)
{
}
