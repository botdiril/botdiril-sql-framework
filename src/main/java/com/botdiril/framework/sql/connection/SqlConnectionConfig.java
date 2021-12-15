package com.botdiril.framework.sql.connection;

public record SqlConnectionConfig(
    String host,
    String username,
    String password,
    String defaultSchema
)
{
}
