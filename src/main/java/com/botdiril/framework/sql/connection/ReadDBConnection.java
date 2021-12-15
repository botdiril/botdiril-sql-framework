package com.botdiril.framework.sql.connection;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;

import com.botdiril.framework.sql.ISqlCallback;
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


    private Optional<byte[]> retrieveBlob(ResultSet resultSet, String columnName) throws SQLException, IOException
    {
        var blob = resultSet.getBlob(columnName);

        if (resultSet.wasNull())
        {

            blob.free();
            return Optional.empty();
        }

        try (var is = blob.getBinaryStream())
        {
            var bytes = new byte[(int) blob.length()];

            if (is.read(bytes) != bytes.length)
                throw new DBException("Blob read size mismatch.");

            return Optional.of(bytes);
        }
        finally
        {
            blob.free();
        }
    }

    protected <R> Optional<R> retrieveValue(ResultSet resultSet, String columnName, Class<R> valueType) throws SQLException, IOException
    {
        if (valueType == byte[].class)
        {
            return this.retrieveBlob(resultSet, columnName).map(valueType::cast);
        }

        Object val;

        if (valueType == Integer.class)
        {
            val = resultSet.getInt(columnName);
        }
        else if (valueType == Long.class)
        {
            val = resultSet.getLong(columnName);
        }
        else if (valueType == Float.class)
        {
            val = resultSet.getFloat(columnName);
        }
        else if (valueType == Double.class)
        {
            val = resultSet.getDouble(columnName);
        }
        else if (valueType == String.class)
        {
            val = resultSet.getString(columnName);
        }
        else
        {
            throw new UnsupportedOperationException(String.format("Unsupported type %s.", valueType.getName()));
        }

        return resultSet.wasNull() ? Optional.empty() : Optional.of(valueType.cast(val));
    }

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
