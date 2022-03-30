package com.botdiril.framework.sql.orm.column;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.botdiril.framework.sql.orm.column.defaultvalue.DefaultValueSupplier;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface DefaultValue
{
    @SuppressWarnings("rawtypes")
    Class<? extends DefaultValueSupplier> value();

    @Language(value = "MySQL", prefix = "SELECT ", suffix = " FROM dual") String expression() default "";
}

