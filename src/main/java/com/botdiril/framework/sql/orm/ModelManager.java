package com.botdiril.framework.sql.orm;

import java.beans.PropertyVetoException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.framework.sql.connection.SqlConnectionManager;
import com.botdiril.framework.sql.connection.WriteDBConnection;
import com.botdiril.framework.sql.orm.column.*;
import com.botdiril.framework.sql.orm.column.bounds.DecimalPrecision;
import com.botdiril.framework.sql.orm.column.defaultvalue.DefaultValueSupplier;
import com.botdiril.framework.sql.orm.column.defaultvalue.ExpressionDefaultValueSupplier;
import com.botdiril.framework.sql.orm.schema.Schema;
import com.botdiril.framework.sql.orm.table.Table;
import com.botdiril.framework.sql.util.SqlLogger;

public class ModelManager implements AutoCloseable
{
    private enum Phase
    {
        INITIAL,
        INITIALIZED
    }

    private static final Map<Class<?>, Function<int[], ?>> COLUMN_BOUNDS_PROVIDERS = new HashMap<>();

    static
    {
        COLUMN_BOUNDS_PROVIDERS.put(BigDecimal.class, bounds -> {
            if (bounds.length != 2)
                throw new IllegalArgumentException("Expecting two arguments exactly (precision, scale).");

            return new DecimalPrecision(bounds[0], bounds[1]);
        });

        Function<int[], Integer> optionalValueExtractor = bounds -> {
            if (bounds.length == 0)
                return null;

            if (bounds.length != 1)
                throw new IllegalArgumentException("Expecting one or zero values.");

            return bounds[0];
        };

        COLUMN_BOUNDS_PROVIDERS.put(int.class, optionalValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(Integer.class,optionalValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(long.class, optionalValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(Long.class, optionalValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(float.class, optionalValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(Float.class, optionalValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(double.class, optionalValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(Double.class, optionalValueExtractor);

        Function<int[], Integer> oneValueExtractor = bounds -> {
            if (bounds.length != 1)
                throw new IllegalArgumentException("Expecting one value exactly.");

            return bounds[0];
        };


        COLUMN_BOUNDS_PROVIDERS.put(byte[].class, oneValueExtractor);
        COLUMN_BOUNDS_PROVIDERS.put(String.class, oneValueExtractor);
    }

    private UUID uuid;
    private Phase state;
    private final SqlConnectionConfig config;
    private SqlConnectionManager connectionManager;

    private final Map<String, Model> models;

    private record KeyReferenceList<T>(ModelColumn<T> column, List<ForeignKey> keys)
    {

    }

    private final List<KeyReferenceList<?>> deferredForeignKeys;


    public ModelManager(SqlConnectionConfig config)
    {
        this.uuid = UUID.randomUUID();
        this.config = config;
        this.state = Phase.INITIAL;

        this.models = new HashMap<>();

        this.deferredForeignKeys = new ArrayList<>();
    }

    public void registerModels(Class<?> schemaKlass)
    {
        if (this.state != Phase.INITIAL)
            throw new IllegalStateException("Cannot register a model in this state.");

        this.loadModel(schemaKlass);
    }

    @Override
    public void close()
    {
        this.models.clear();
    }

    public Model getModel(String name)
    {
        return this.models.get(name);
    }

    public Map<String, Model> getModels()
    {
        return Collections.unmodifiableMap(this.models);
    }

    private void loadModel(Class<?> klass)
    {
        var modelAnnotation = klass.getAnnotation(Schema.class);

        if (modelAnnotation == null)
            return;

        var modelName = modelAnnotation.name();

        var model = new Model(modelName, modelAnnotation, klass, this);

        var classes = klass.getDeclaredClasses();

        for (var tableKlass : classes)
        {
            try
            {
                this.loadTable(tableKlass, model);
            }
            catch (Throwable e)
            {
                SqlLogger.instance.error(e);
            }
        }

        this.models.put(modelName, model);
    }

    private void loadTable(Class<?> klass, Model model) throws Throwable
    {
        var tableAnnotation = klass.getAnnotation(Table.class);

        if (tableAnnotation == null)
            return;

        var tableName = tableAnnotation.name();

        var lookup = MethodHandles.lookup();

        MethodHandle tableConstructor;

        try
        {
            tableConstructor = lookup.findConstructor(klass, MethodType.methodType(void.class));
        }
        catch (NoSuchMethodException e)
        {
            SqlLogger.instance.error("Table `{}` in `{}` does not have a public no-arg constructor.", tableName, klass);
            return;
        }

        var tableObj = tableConstructor.invoke();

        var constructor = lookup.findConstructor(ModelTable.class, MethodType.methodType(void.class, String.class, Object.class, Table.class, Class.class, Model.class));

        var table = (ModelTable<?>) constructor.invoke(tableName, tableObj, tableAnnotation, klass, model);

        var fields = klass.getDeclaredFields();

        for (var field : fields)
            this.loadColumn(field, table);

        model.addTable(table);
    }

    private void loadColumn(Field field, ModelTable<?> table) throws Throwable
    {
        var columnAnnotation = field.getAnnotation(Column.class);

        if (columnAnnotation == null)
            return;

        var tableMeta = table.getTableMeta();
        var assignedName = columnAnnotation.name();
        var columnNameRaw = assignedName.isBlank() ? field.getName() : assignedName;
        var columnName = tableMeta.prefix() + "_" + columnNameRaw;
        var columnDataType = columnAnnotation.dataType();

        var boundsProvider = COLUMN_BOUNDS_PROVIDERS.get(columnDataType);

        var boundsRaw = columnAnnotation.bounds();
        // Pass-through if no provider found
        var bounds = boundsProvider != null ? boundsProvider.apply(boundsRaw) : boundsRaw;
        var columnInfo = ColumnInfo.of(columnName, columnDataType, bounds);

        var foundPrimaryKey = false;

        var flags = EnumSet.noneOf(EnumColumnFlag.class);

        for (var flag : EnumColumnFlag.values())
        {
            var flagAnnotation = flag.getAnnotationClass();

            if (field.getAnnotation(flagAnnotation) == null)
                continue;

            if (flagAnnotation == PrimaryKey.class)
            {
                if (foundPrimaryKey)
                   throw new IllegalArgumentException("Only one primary key is allowed.");

                foundPrimaryKey = true;
            }

            flags.add(flag);
        }

        var lookup = MethodHandles.lookup();

        var defaultValueAnnotation = field.getAnnotation(DefaultValue.class);

        var defaultValueSupplier = (DefaultValueSupplier<?>) null;

        if (defaultValueAnnotation != null)
        {
            var defaultValueSupplierClass = defaultValueAnnotation.value();

            if (defaultValueSupplierClass == ExpressionDefaultValueSupplier.class)
            {
                defaultValueSupplier = new ExpressionDefaultValueSupplier(defaultValueAnnotation.expression());
            }
            else
            {
                try
                {
                    var supplierCtor = lookup.findConstructor(defaultValueSupplierClass, MethodType.methodType(void.class));
                    defaultValueSupplier = (DefaultValueSupplier<?>) supplierCtor.invoke();
                }
                catch (NoSuchMethodException e)
                {
                    throw new NoSuchElementException("Default value supplier `%s` does not have a public no-arg constructor.".formatted(defaultValueSupplierClass));
                }
            }
        }

        var constructor = lookup.findConstructor(ModelColumn.class, MethodType.methodType(void.class, String.class, Column.class, ColumnInfo.class, ModelTable.class, EnumSet.class, DefaultValueSupplier.class));

        var column = (ModelColumn<?>) constructor.invoke(columnName, columnAnnotation, columnInfo, table, flags, defaultValueSupplier);

        var foreignKeyAnnotations = field.getAnnotationsByType(ForeignKey.class);

        if (foreignKeyAnnotations.length != 0)
        {
            var keys = new KeyReferenceList<>(column, Arrays.asList(foreignKeyAnnotations));

            this.deferredForeignKeys.add(keys);
        }

        if (Modifier.isStatic(field.getModifiers()))
            field.set(null, column);
        else
            SqlLogger.instance.error("Field `{}` in `{}` is not static, will not be initialized.", field, table.getTableClass());


        table.addColumn(column);
    }

    public void initialize() throws SQLException, PropertyVetoException
    {
        if (this.state != Phase.INITIAL)
            throw new IllegalStateException("Cannot initialize a model manager in this state.");

        for (var fk : this.deferredForeignKeys)
        {
            var column = fk.column();

            var keys = fk.keys();

            for (var key : keys)
            {
                var targetTblClass = key.value();
                var targetSchemaClass = targetTblClass.getDeclaringClass();
                var deleteAction = key.parentDeleteAction();

                var modelAnnotation = targetSchemaClass.getAnnotation(Schema.class);
                var tableAnnotation = targetTblClass.getAnnotation(Table.class);

                var modelName = modelAnnotation.name();
                var tableName = tableAnnotation.name();

                var targetModel = this.models.get(modelName);
                assert targetModel != null;

                var targetTable = targetModel.getTable(tableName);
                assert targetTable != null;

                var primaryColumn = targetTable.getPrimaryKeyColumn();

                if (primaryColumn.isEmpty())
                {
                    var srcTbl = column.getTable();
                    var srcSchema = srcTbl.getSchema();

                    throw new IllegalStateException("Column `%s`.`%s`.`%s` refers to the table `%s`.`%s`, which doesn't have a primary key!".formatted(
                        srcSchema.getName(), srcTbl.getName(), column.getName(), modelName, tableName
                    ));
                }

                column.addForeignKey(primaryColumn.get(), deleteAction);
            }
        }

        var jdbcURL = "jdbc:mysql://" + this.config.host()
                + "/?useUnicode=true"
                + "&autoReconnect=true"
                + "&useJDBCCompliantTimezoneShift=true"
                + "&useLegacyDatetimeCode=false"
                + "&serverTimezone=UTC";

        try (var c = DriverManager.getConnection(jdbcURL, this.config.username(), this.config.password()))
        {
            try (var db = WriteDBConnection.fromExisting(c))
            {
                db.createSchema(this.config.defaultSchema());

                this.models.forEach((name, model) -> model.build(db));

                this.models.values()
                           .stream()
                           .map(Model::getTables)
                           .flatMap(Collection::stream)
                           .map(ModelTable::getColumns)
                           .flatMap(Collection::stream)
                           .forEach(column -> column.buildForeignKeys(db));

                db.commit();
            }
        }

        this.state = Phase.INITIALIZED;

        this.connectionManager = new SqlConnectionManager(this.config);
    }

    public SqlConnectionManager getConnectionManager()
    {
        if (this.state != Phase.INITIALIZED)
            throw new IllegalStateException("Cannot return a connection manager in this state.");

        return this.connectionManager;
    }

    public UUID getUUID()
    {
        return this.uuid;
    }

    public void setUUID(UUID uuid)
    {
        this.uuid = uuid;
    }

    @Override
    public String toString()
    {
        var modelStr = this.models.values()
                                  .stream()
                                  .map(Model::toString)
                                  .collect(Collectors.joining("\n\n"));

        return """
        model-manager %s {
        %s}""".formatted(this.uuid, modelStr.indent(2));
    }
}
