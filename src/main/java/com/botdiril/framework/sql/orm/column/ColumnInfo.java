package com.botdiril.framework.sql.orm.column;

import com.mysql.cj.MysqlType;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.orm.column.bounds.DecimalPrecision;
import com.botdiril.framework.sql.orm.types.EnumDataType;

public record ColumnInfo<T>(String name, MysqlType type, Class<T> javaType, Object bounds)
{
    public ColumnInfo
    {
        var requiredType = switch (type) {
            case INT, BIGINT, VARCHAR, BLOB, FLOAT, DOUBLE -> Integer.class;
            case ENUM, SET -> Class.class;
            case DECIMAL -> DecimalPrecision.class;
            default -> null;
        };

        if (requiredType == null ^ bounds == null)
            throw new IllegalArgumentException("Data type bounds type mismatch.");

        if (bounds != null && !requiredType.isInstance(bounds))
            throw new IllegalArgumentException("Data type bounds type mismatch.");
    }

    public static <T> ColumnInfo<T> of(String name, Class<T> javaType)
    {
        return of(name, javaType, null);
    }

    public static <T> ColumnInfo<T> of(String name, Class<T> javaType, Object bounds)
    {
        var dataType = EnumDataType.getByClass(javaType);

        return new ColumnInfo<>(name, dataType.getJDBCType(), javaType, switch (dataType) {
            case INT -> Objects.requireNonNullElse(bounds, 11);
            case BIGINT -> Objects.requireNonNullElse(bounds, 20);
            case FLOAT -> Objects.requireNonNullElse(bounds, 24);
            case DOUBLE ->  Objects.requireNonNullElse(bounds, 53);
            case DECIMAL -> Objects.requireNonNullElse(bounds, new DecimalPrecision(10, 0));
            default -> bounds;
        });
    }

    @Override
    public String toString()
    {
        var sb = new StringBuilder();
        sb.append('`');
        sb.append(this.name);
        sb.append("` ");
        sb.append(this.type.getName());

        switch (this.type)
        {
            case INT, BIGINT, VARCHAR, BLOB, FLOAT, DOUBLE -> {
                sb.append('(');
                sb.append(this.bounds);
                sb.append(')');
            }

            case ENUM, SET -> {
                sb.append('(');
                var enumConstants =  ((Class<?>) this.bounds).getEnumConstants();
                var names = Arrays.stream(enumConstants)
                                  .map(Enum.class::cast)
                                  .map(Enum::name)
                                  .collect(Collectors.joining(","));

                sb.append(names);
                sb.append(')');
            }

            case DECIMAL -> {
                sb.append('(');
                var precision = (DecimalPrecision) this.bounds;
                sb.append(precision.significantDigits());
                sb.append(',');
                sb.append(precision.scale());
                sb.append(')');
            }
        }

        return sb.toString();
    }
}
