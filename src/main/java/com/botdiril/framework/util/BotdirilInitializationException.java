package com.botdiril.framework.util;

public class BotdirilInitializationException extends RuntimeException
{
    public BotdirilInitializationException(String message, Throwable cause)
    {
        super(message, cause);
    }

    public BotdirilInitializationException(Throwable cause)
    {
        super(cause);
    }

    public BotdirilInitializationException(String message)
    {
        super(message);
    }
}
