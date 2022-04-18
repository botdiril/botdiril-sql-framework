package com.botdiril.framework.sql.connection;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.RecordComponent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;

import com.botdiril.framework.sql.ISqlCallback;
import com.botdiril.framework.sql.orm.ModelColumn;
import com.botdiril.framework.sql.orm.types.EnumDataType;
import com.botdiril.framework.sql.util.DBException;

public class ReadDBConnection extends AbstractDBConnection
{
    ReadDBConnection(Connection connection, boolean autocommit)
    {
        super(connection, autocommit, true);
    }

    protected ReadDBConnection(Connection connection, boolean autocommit, boolean readOnly)
    {
        super(connection, autocommit, readOnly);
    }

    public static ReadDBConnection fromExisting(Connection c)
    {
        try
        {
            return new ReadDBConnection(c, c.getAutoCommit(), true);
        }
        catch (SQLException e)
        {
            throw new DBException(e);
        }
    }

    /// Single value retrieval

    protected <R> Optional<R> retrieveValue(ResultSet resultSet, String columnName, Class<R> valueType) throws SQLException, IOException
    {
        var type = EnumDataType.getByClass(valueType);
        var extractor = type.getExtractor();

        if (extractor == null)
            throw new UnsupportedOperationException(String.format("Unsupported type %s.", valueType.getName()));

        var val = extractor.extract(resultSet, columnName);

        return resultSet.wasNull() ? Optional.empty() : Optional.of(valueType.cast(val));
    }

    /// Non-ORM retrieval

    public <R> @NotNull Optional<R> getValue(@Language("MySQL") String statement, String columnName, Class<R> valueType, Object... params)
    {
        return this.query(statement, rs -> {
            if (!rs.next())
                return Optional.empty();

            return this.retrieveValue(rs, columnName, valueType);
        }, params);
    }

    public <R> R getValueOr(@Language("MySQL") String statement, String columnName, Class<R> valueType, R fallbackValue, Object... params)
    {
        return this.query(statement, rs -> {
            if (!rs.next())
                return fallbackValue;

            return this.retrieveValue(rs, columnName, valueType).orElse(fallbackValue);
        }, params);
    }

    public <R> R getValueOrNull(@Language("MySQL") String statement, String columnName, Class<R> valueType, Object... params)
    {
        return this.getValueOr(statement, columnName, valueType, null, params);
    }

    public <R> List<R> getList(@Language("MySQL") String statement, String columnName, Class<R> valueType, Object... params)
    {
        return this.query(statement, rs -> {
            var resultList = new ArrayList<R>();
            Optional<R> val;

            while (rs.next())
            {
                val = this.retrieveValue(rs, columnName, valueType);

                if (val.isEmpty())
                    continue;

                resultList.add(val.get());
            }

            return Collections.unmodifiableList(resultList);
        }, params);
    }

    public <KT, VT> Map<KT, VT> getMap(@Language("MySQL") String statement, String keyColumn, Class<KT> keyType, String valueColumn, Class<VT> valueType, Object... params)
    {
        return this.query(statement, rs -> {
            var resultList = new HashMap<KT, VT>();
            Optional<KT> key;
            Optional<VT> val;

            while (rs.next())
            {
                key = this.retrieveValue(rs, keyColumn, keyType);
                val = this.retrieveValue(rs, valueColumn, valueType);

                if (key.isEmpty())
                    continue;

                resultList.put(key.get(), val.orElse(null));
            }

            return Collections.unmodifiableMap(resultList);
        }, params);
    }

    public <KT, VT> List<Pair<KT, VT>> getPairs(@Language("MySQL") String statement, String keyColumn, Class<KT> keyType, String valueColumn, Class<VT> valueType, Object... params)
    {
        return this.query(statement, rs -> {
            var resultList = new ArrayList<Pair<KT, VT>>();

            Optional<KT> key;
            Optional<VT> val;

            while (rs.next())
            {
                key = this.retrieveValue(rs, keyColumn, keyType);
                val = this.retrieveValue(rs, valueColumn, valueType);

                resultList.add(ImmutablePair.of(key.orElse(null), val.orElse(null)));
            }

            return resultList;
        }, params);
    }

    /// ORM-based retrieval

    public <R> @NotNull Optional<R> getValue(@Language("MySQL") String statement, ModelColumn<R> column, Object... params)
    {
        var info = column.getInfo();
        return this.getValue(statement, info.name(), info.javaType(), params);
    }

    public <R> R getValueOr(@Language("MySQL") String statement, ModelColumn<R> column, R fallbackValue, Object... params)
    {
        var info = column.getInfo();
        return this.getValueOr(statement, info.name(), info.javaType(), fallbackValue, params);
    }

    public <R> R getValueOrNull(@Language("MySQL") String statement, ModelColumn<R> column, Object... params)
    {
        var info = column.getInfo();
        return this.getValueOrNull(statement, info.name(), info.javaType(), params);
    }

    public <R> List<R> getList(@Language("MySQL") String statement, ModelColumn<R> column, Object... params)
    {
        var info = column.getInfo();
        return this.getList(statement, info.name(), info.javaType(), params);
    }

    public <KT, VT> Map<KT, VT> getMap(@Language("MySQL") String statement, ModelColumn<KT> keyColumn, ModelColumn<VT> valueColumn, Object... params)
    {
        var keyInfo = keyColumn.getInfo();
        var valueInfo = valueColumn.getInfo();
        return this.getMap(statement, keyInfo.name(), keyInfo.javaType(), valueInfo.name(), valueInfo.javaType(), params);
    }

    public <KT, VT> List<Pair<KT, VT>> getPairs(@Language("MySQL") String statement, ModelColumn<KT> leftColumn, ModelColumn<VT> rightColumn, Object... params)
    {
        var leftInfo = leftColumn.getInfo();
        var rightInfo = rightColumn.getInfo();
        return this.getPairs(statement, leftInfo.name(), leftInfo.javaType(), rightInfo.name(), rightInfo.javaType(), params);
    }

    /// Reflective record automatic databinding

    private <R extends Record> @NotNull ISqlCallback<R, ResultSet> createRecordExtractor(Class<R> recordType)
    {
        var components = recordType.getRecordComponents();

        Class<?>[] paramTypes = Arrays.stream(components)
                                      .map(RecordComponent::getType)
                                      .toArray(Class<?>[]::new);

        var lookup = MethodHandles.publicLookup();

        try
        {
            var ctor = lookup.findConstructor(recordType, MethodType.methodType(void.class, paramTypes));

            Function<RecordComponent, ISqlCallback<?, ResultSet>> valueExtractor = comp -> rs -> this.retrieveValue(rs, comp.getName(), comp.getType()).orElse(null);

            var valueExtractors = Arrays.stream(components)
                                        .sequential()
                                        .map(valueExtractor)
                                        .toList();

            return (rs) -> {
                var args = valueExtractors.stream()
                                          .map(ext -> ext.partialUnchecked(rs))
                                          .map(ISqlCallback.ISqlConsumerUnchecked::exec)
                                          .toList();

                return recordType.cast(ctor.invokeWithArguments(args));
            };
        }
        catch (ReflectiveOperationException e)
        {
            throw new IllegalArgumentException(e);
        }
    }

    public <R extends Record> @NotNull Optional<R> getRecord(@Language("MySQL") String statement, Class<R> recordType, Object... params)
    {
        var recordExtractor = createRecordExtractor(recordType);

        return this.query(statement, rs -> {
            if (!rs.next())
                return Optional.empty();

            return Optional.of(recordExtractor.exec(rs));
        }, params);
    }

    public <R extends Record> R getRecordOr(@Language("MySQL") String statement, Class<R> recordType, R fallbackValue, Object... params)
    {
        var recordExtractor = createRecordExtractor(recordType);

        return this.query(statement, rs -> {
            if (!rs.next())
                return fallbackValue;

            return recordExtractor.exec(rs);
        }, params);
    }

    public <R extends Record> List<R> getRecordList(@Language("MySQL") String statement, Class<R> recordType, Object... params)
    {
        var recordExtractor = createRecordExtractor(recordType);

        return this.query(statement, rs -> {
            var resultList = new ArrayList<R>();

            while (rs.next())
                resultList.add(recordExtractor.exec(rs));

            return Collections.unmodifiableList(resultList);
        }, params);
    }

    public <R> R query(@Language("MySQL") String statement, ISqlCallback<R, ResultSet> callback, Object... params)
    {
        return this.executeStatement(statement, PreparedStatement::executeQuery, callback, params);
    }

    public boolean hasRow(@Language("MySQL") String statement, Object... params)
    {
        return this.query(statement, ResultSet::next, params);
    }

    public boolean schemaExists(String name)
    {
        return this.hasRow("SHOW DATABASES LIKE ?", name);
    }

    public boolean tableExists(String name)
    {
        return this.hasRow("SHOW TABLES LIKE ?", name);
    }
}
