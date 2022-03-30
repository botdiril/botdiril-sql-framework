package com.botdiril.framework.sql.orm.types;

import com.mysql.cj.MysqlType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.*;

import com.botdiril.framework.sql.util.DBException;

public enum EnumDataType
{
    BOOLEAN(MysqlType.BOOLEAN, boolean.class, Set.of(boolean.class, Boolean.class), ResultSet::getBoolean, PreparedStatement::setBoolean),

    INT(MysqlType.INT, int.class, Set.of(int.class, Integer.class), ResultSet::getInt, PreparedStatement::setInt),

    BIGINT(MysqlType.BIGINT, long.class, Set.of(long.class, Long.class), ResultSet::getLong, PreparedStatement::setLong),

    FLOAT(MysqlType.FLOAT, float.class, Set.of(float.class, Float.class), ResultSet::getFloat, PreparedStatement::setFloat),

    DOUBLE(MysqlType.DOUBLE, double.class, Set.of(double.class, Double.class), ResultSet::getDouble, PreparedStatement::setDouble),

    VARCHAR(MysqlType.VARCHAR, String.class, Set.of(String.class), ResultSet::getString, PreparedStatement::setString),

    BLOB(MysqlType.BLOB, byte[].class, Set.of(byte[].class), EnumDataType::retrieveBlob, (statement, idx, value) -> {
        ByteArrayInputStream stream = new ByteArrayInputStream(value);
        statement.setBinaryStream(idx, stream);
    }),

    DECIMAL(MysqlType.DECIMAL, BigDecimal.class, Set.of(BigDecimal.class), ResultSet::getBigDecimal, PreparedStatement::setBigDecimal),

    LOCAL_DATE_TIME(MysqlType.TIMESTAMP, LocalDateTime.class, Set.of(LocalDateTime.class),
        (resultSet, column) -> resultSet.getTimestamp(column).toLocalDateTime(),
        PreparedStatement::setObject),

    ENUM(MysqlType.ENUM, Enum.class, Set.of(Enum.class), null, null),

    SET(MysqlType.SET, EnumSet.class, Set.of(EnumSet.class), null, null);

    private static final Map<Class<?>, EnumDataType> CLASS_MAP = new HashMap<>();
    private static final Map<MysqlType, EnumDataType> JDBC_MAP = new HashMap<>();

    static
    {
        for (var dt : values())
        {
            dt.classes.forEach(k -> CLASS_MAP.put(k, dt));
            JDBC_MAP.put(dt.type, dt);
        }
    }

    private final MysqlType type;
    private final Class<?> rootClass;
    private final Set<Class<?>> classes;
    private final ValueExtractor<?> extractor;
    private final ValueWriter<?> writer;

    <T> EnumDataType(MysqlType type, Class<T> rootClass, Set<Class<?>> classes, ValueExtractor<T> extractor, ValueWriter<T> writer)
    {
        this.type = type;
        this.rootClass = rootClass;
        this.classes = classes;
        this.extractor = extractor;
        this.writer = writer;
    }

    public static EnumDataType getByClass(Class<?> klass)
    {
        return CLASS_MAP.get(klass);
    }

    public Set<Class<?>> getClasses()
    {
        return Collections.unmodifiableSet(this.classes);
    }

    public Class<?> getRootClass()
    {
        return this.rootClass;
    }

    public static EnumDataType getByJDBC(MysqlType mysqlType)
    {
        return JDBC_MAP.get(mysqlType);
    }

    public MysqlType getJDBCType()
    {
        return this.type;
    }

    @SuppressWarnings("unchecked")
    public ValueExtractor<Object> getExtractor()
    {
        return (ValueExtractor<Object>) this.extractor;
    }

    @SuppressWarnings("unchecked")
    public ValueWriter<Object> getWriter()
    {
        return (ValueWriter<Object>) this.writer;
    }

    @FunctionalInterface
    public interface ValueExtractor<T>
    {
        T extract(ResultSet resultSet, String column) throws IOException, SQLException;
    }

    @FunctionalInterface
    public interface ValueWriter<T>
    {
        void write(PreparedStatement statement, int idx, T value) throws IOException, SQLException;
    }

    private static byte[] retrieveBlob(ResultSet resultSet, String columnName) throws SQLException, IOException
    {
        var blob = resultSet.getBlob(columnName);

        if (resultSet.wasNull())
        {
            blob.free();
            return null;
        }

        try (var is = blob.getBinaryStream())
        {
            var size = (int) blob.length();
            var bytes = new byte[size];

            if (is.read(bytes) != bytes.length)
                throw new DBException("Blob read size mismatch.");

            return bytes;
        }
        finally
        {
            blob.free();
        }
    }

}
