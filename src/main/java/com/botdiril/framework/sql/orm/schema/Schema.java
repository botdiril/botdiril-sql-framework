package com.botdiril.framework.sql.orm.schema;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Schema
{
    String name();
}
