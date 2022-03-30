package com.botdiril.framework.sql.orm.column.defaultvalue;

import org.intellij.lang.annotations.Language;

public class ExpressionDefaultValueSupplier extends DefaultValueSupplier<String>
{
    private final @Language(value = "MySQL", prefix = "SELECT ", suffix = " FROM dual") String sqlExpression;

    public ExpressionDefaultValueSupplier(@Language(value = "MySQL", prefix = "SELECT ", suffix = " FROM dual") String code)
    {
        this.sqlExpression = code;
    }

    @Override
    public String get()
    {
        return this.sqlExpression;
    }
}
