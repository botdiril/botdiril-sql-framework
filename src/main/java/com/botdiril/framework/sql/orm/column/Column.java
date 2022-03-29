package com.botdiril.framework.sql.orm.column;


import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Column
{
    /**
     * The column name, extracted from the field name when null.
     */
    String name() default "";

    int[] bounds() default {};

    Class<?> dataType();
}
