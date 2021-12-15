package com.botdiril.framework.sql.orm.column;

import org.intellij.lang.annotations.Language;

import com.botdiril.framework.sql.orm.column.defaultvalue.DefaultValueSupplier;

public @interface DefaultValue
{
    Class<? extends DefaultValueSupplier<?>> value();

    @Language(value = "MySQL", prefix = "SELECT ", suffix = " FROM dual") String expression() default "";
}
