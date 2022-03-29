package com.botdiril.framework.sql.orm.column;

import java.lang.annotation.Annotation;
import java.util.Set;

public enum EnumColumnFlag
{
    PRIMARY_KEY("PRIMARY KEY", PrimaryKey.class, null),
    AUTO_INCREMENT("AUTO_INCREMENT", AutoIncrement.class, Set.of(int.class, Integer.class, long.class, Long.class)),
    UNIQUE("UNIQUE", Unique.class, null),
    NOT_NULL("NOT NULL", NotNull.class, null);

    private final String name;
    private final Class<? extends Annotation> annotationClass;
    private final Set<Class<?>> allowedTypes;

    EnumColumnFlag(String name, Class<? extends Annotation> relevantAnnotation, Set<Class<?>> allowedTypeSet)
    {
        this.name = name;
        // Null for "allow all"
        this.allowedTypes = allowedTypeSet;

        this.annotationClass = relevantAnnotation;
    }

    public String getCreateInfo()
    {
        return this.name;
    }

    public Class<? extends Annotation> getAnnotationClass()
    {
        return this.annotationClass;
    }

    public boolean isValidForType(Class<?> klass)
    {
        if (this.allowedTypes == null)
            return true;

        return this.allowedTypes.contains(klass);
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}
