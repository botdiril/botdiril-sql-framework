package com.botdiril.framework.sql.orm.column;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
@Repeatable(ForeignKeyList.class)
public @interface ForeignKey
{
    enum ParentDeleteAction
    {
        NONE("ON DELETE RESTRICT"),
        CASCADE_DELETE("ON DELETE CASCADE"),
        SET_NULL("ON DELETE SET NULL");

        @Language(value = "MySQL", prefix = "ALTER TABLE dual ADD FOREIGN KEY dummy REFERENCES dummy_table(dummy_pk) ")
        private final String createInfo;

        ParentDeleteAction(@Language(value = "MySQL", prefix = "ALTER TABLE dual ADD FOREIGN KEY dummy REFERENCES dummy_table(dummy_pk) ") String createInfo)
        {
            this.createInfo = createInfo;
        }

        @Language(value = "MySQL", prefix = "ALTER TABLE dual ADD FOREIGN KEY dummy REFERENCES dummy_table(dummy_pk) ")
        public String getCreateInfo()
        {
            return this.createInfo;
        }
    }

    /**
     * The parent table.
     */
    Class<?> value();

    /**
     * Specifies what should happen to the row when this column's parent entry is deleted.
     */
    ParentDeleteAction parentDeleteAction() default ParentDeleteAction.NONE;
}
