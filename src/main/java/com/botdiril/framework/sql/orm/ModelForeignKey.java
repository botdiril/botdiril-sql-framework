package com.botdiril.framework.sql.orm;

import org.intellij.lang.annotations.MagicConstant;

import com.botdiril.framework.sql.orm.column.ForeignKey;

public record ModelForeignKey(ModelColumn<?> column,
                              @MagicConstant(flagsFromClass = ForeignKey.ParentDeleteAction.class) int parentDeleteAction)
{
}
