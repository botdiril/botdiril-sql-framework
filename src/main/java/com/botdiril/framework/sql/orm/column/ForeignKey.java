package com.botdiril.framework.sql.orm.column;

import org.intellij.lang.annotations.MagicConstant;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ForeignKeyList.class)
public @interface ForeignKey
{
    class ParentDeleteAction
    {
        public static final int NONE = 0;
        public static final int CASCADE_DELETE = 1;
        public static final int SET_NULL = 2;
        public static final int SET_DEFAULT = 3;
    }

    /**
     * The parent table.
     */
    Class<?> value();

    /**
     * Specifies what should happen to the row when this column's parent entry is deleted.
     */
    @MagicConstant(flagsFromClass = ParentDeleteAction.class)
    int parentDeleteAction() default ParentDeleteAction.NONE;
}
