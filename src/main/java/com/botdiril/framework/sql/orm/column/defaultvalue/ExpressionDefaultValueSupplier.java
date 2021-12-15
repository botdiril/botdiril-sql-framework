package com.botdiril.framework.sql.orm.column.defaultvalue;

import org.intellij.lang.annotations.Language;

import com.botdiril.framework.sql.util.SqlExpression;

public class ExpressionDefaultValueSupplier<T> extends DefaultValueSupplier<SqlExpression<T>>
{
    private final SqlExpression<T> sqlExpression;

    protected ExpressionDefaultValueSupplier(@Language(value = "MySQL", prefix = "SELECT ", suffix = " FROM dual") String code)
    {
        this.sqlExpression = new SqlExpression<>(code);
    }

    @Override
    public SqlExpression<T> get()
    {
        return this.sqlExpression;
    }
}
