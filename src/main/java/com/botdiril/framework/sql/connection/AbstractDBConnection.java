package com.botdiril.framework.sql.connection;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

import com.botdiril.framework.sql.IDBResource;
import com.botdiril.framework.sql.ISqlCallback;
import com.botdiril.framework.sql.ISqlExecuteFunction;
import com.botdiril.framework.sql.orm.types.EnumDataType;
import com.botdiril.framework.sql.util.DBException;
import com.botdiril.framework.sql.util.ParamNull;
import com.botdiril.framework.sql.util.SqlLogger;

public abstract class AbstractDBConnection implements IDBResource
{
    protected final Connection connection;
    protected final boolean autocommit;
    protected boolean readOnly;

    protected AbstractDBConnection(Connection connection, boolean autocommit, boolean readOnly)
    {
        this.connection = connection;
        this.autocommit = autocommit;
        this.readOnly = readOnly;
    }

    protected void setParams(PreparedStatement statement, Object... params) throws Exception
    {
        for (int i = 0; i < params.length; i++)
        {
            var param = params[i];

            if (param == null)
            {
                throw new IllegalStateException("Parameter can't be raw null!");
            }

            int paramIdx = i + 1;

            if (param instanceof ParamNull paramNull)
            {
                statement.setNull(paramIdx, paramNull.type().getJdbcType());
                continue;
            }

            var klass = param.getClass();
            var type = EnumDataType.getByClass(klass);

            if (type == null)
                throw new UnsupportedOperationException("Unsupported DB data type.");

            var writer = type.getWriter();

            if (writer == null)
                throw new UnsupportedOperationException("Unsupported DB data type.");

            writer.write(statement, paramIdx, param);
        }
    }

    protected <R, V> R executeStatement(boolean generatedKeys, @Language("MySQL") String statement, ISqlExecuteFunction<V> executeMethod, ISqlCallback<R, V> callback, Object... params)
    {
        try
        {
            try (var stat = this.connection.prepareStatement(statement, generatedKeys ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS))
            {
                this.setParams(stat, params);

                SqlLogger.logStatement(stat);

                return callback.exec(executeMethod.apply(stat));
            }
        }
        catch (Throwable e)
        {
            throw new DBException(e);
        }
    }

    protected <R, V> R executeStatement(@Language("MySQL") String statement, ISqlExecuteFunction<V> executeMethod, ISqlCallback<R, V> callback, Object... params)
    {
        return this.executeStatement(false, statement, executeMethod, callback, params);
    }

    @Override
    public void close()
    {
        try
        {
            this.rollback();

            this.connection.close();
        }
        catch (SQLException e)
        {
            throw new DBException(e);
        }
    }

    public void commit()
    {
        if (this.autocommit)
            return;

        try
        {
            this.connection.commit();
        }
        catch (SQLException e)
        {
            throw new DBException(e);
        }
    }

    public void rollback()
    {
        if (this.autocommit)
            return;

        try
        {
            this.connection.rollback();
        }
        catch (SQLException e)
        {
            throw new DBException(e);
        }
    }

    public boolean isAutoCommiting()
    {
        return this.autocommit;
    }

    public boolean isReadOnly()
    {
        return this.readOnly;
    }
}
