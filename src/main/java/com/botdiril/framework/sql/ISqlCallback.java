package com.botdiril.framework.sql;

import com.botdiril.framework.sql.util.DBException;

@FunctionalInterface
public interface ISqlCallback<R, V>
{
    R exec(V value) throws Throwable;

    default ISqlConsumerUnchecked<R> partialUnchecked(V value)
    {
        return () -> {
          try
          {
              return this.exec(value);
          }
          catch (Throwable t)
          {
              throw new DBException(t);
          }
        };
    }

    @FunctionalInterface
    interface ISqlConsumerUnchecked<R>
    {
        R exec();
    }
}
