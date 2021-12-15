package com.botdiril.framework.sql;

@FunctionalInterface
public interface ISqlCallback<R, V>
{
    R exec(V executionResult) throws Exception;
}
