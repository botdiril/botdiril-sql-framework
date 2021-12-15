package com.botdiril.framework.sql.util;

import org.intellij.lang.annotations.Language;

public record SqlExpression<T>(
    @Language(value = "MySQL", prefix = "SELECT ", suffix = " FROM dual") String code
)
{
}
