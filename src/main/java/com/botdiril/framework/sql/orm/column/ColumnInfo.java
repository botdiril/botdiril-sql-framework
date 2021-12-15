package com.botdiril.framework.sql.orm.column;

import com.mysql.cj.MysqlType;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

public record ColumnInfo<T>(String name, MysqlType type, Class<T> javaType)
{
    private static final Map<Class<?>, MysqlType> MYSQL_TYPE_MAP = new HashMap<>();

    static
    {
        MYSQL_TYPE_MAP.put(boolean.class, MysqlType.BOOLEAN);
        MYSQL_TYPE_MAP.put(Boolean.class, MysqlType.BOOLEAN);
        MYSQL_TYPE_MAP.put(int.class, MysqlType.INT);
        MYSQL_TYPE_MAP.put(Integer.class, MysqlType.INT);
        MYSQL_TYPE_MAP.put(long.class, MysqlType.BIGINT);
        MYSQL_TYPE_MAP.put(Long.class, MysqlType.BIGINT);
        MYSQL_TYPE_MAP.put(float.class, MysqlType.FLOAT);
        MYSQL_TYPE_MAP.put(Float.class, MysqlType.FLOAT);
        MYSQL_TYPE_MAP.put(double.class, MysqlType.DOUBLE);
        MYSQL_TYPE_MAP.put(Double.class, MysqlType.DOUBLE);
        MYSQL_TYPE_MAP.put(String.class, MysqlType.VARCHAR);
        MYSQL_TYPE_MAP.put(byte[].class, MysqlType.BLOB);
        MYSQL_TYPE_MAP.put(Enum.class, MysqlType.ENUM);
        MYSQL_TYPE_MAP.put(EnumSet.class, MysqlType.SET);
    }

    public static <T> ColumnInfo<T> of(String name, Class<T> javaType)
    {
        var mysqlType = MYSQL_TYPE_MAP.get(javaType);
        return new ColumnInfo<>(name, mysqlType, javaType);
    }

    public String getName()
    {
        return this.name;
    }

    public MysqlType getType()
    {
        return this.type;
    }

    public Class<T> getJavaType()
    {
        return this.javaType;
    }
}
