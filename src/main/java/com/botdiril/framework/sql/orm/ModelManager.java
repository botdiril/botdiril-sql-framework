package com.botdiril.framework.sql.orm;

import java.beans.PropertyVetoException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.connection.SqlConnectionConfig;
import com.botdiril.framework.sql.connection.SqlConnectionManager;
import com.botdiril.framework.sql.connection.WriteDBConnection;
import com.botdiril.framework.sql.orm.column.Column;
import com.botdiril.framework.sql.orm.column.ColumnInfo;
import com.botdiril.framework.sql.orm.column.EnumColumnFlag;
import com.botdiril.framework.sql.orm.column.PrimaryKey;
import com.botdiril.framework.sql.orm.column.bounds.DecimalPrecision;
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

    private final UUID uuid;
    private Phase state;
    private final SqlConnectionConfig config;
    private SqlConnectionManager connectionManager;

    private final Map<String, Model> models;

    private final List<ModelCompiler> compilers;

    public ModelManager(SqlConnectionConfig config)
    {
        this.uuid = UUID.randomUUID();
        this.config = config;
        this.state = Phase.INITIAL;

        this.models = new HashMap<>();
        this.compilers = new ArrayList<>();
    }

    public void registerModels(Path modelDir)
    {
        if (this.state != Phase.INITIAL)
            throw new IllegalStateException("Cannot register a model in this state.");

        var mc = new ModelCompiler(modelDir);
        mc.load(this::loadModel);
        this.compilers.add(mc);
    }

    @Override
    public void close()
    {
        this.compilers.forEach(ModelCompiler::close);
        this.compilers.clear();
        this.models.clear();
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

        var constructor = lookup.findConstructor(ModelColumn.class, MethodType.methodType(void.class, String.class, Column.class, ColumnInfo.class, ModelTable.class, EnumSet.class));

        var column = (ModelColumn<?>) constructor.invoke(columnNameRaw, columnAnnotation, columnInfo, table, flags);

        table.addColumn(column);
    }

    public void initialize() throws SQLException, PropertyVetoException
    {
        if (this.state != Phase.INITIAL)
            throw new IllegalStateException("Cannot initialize a model manager in this state.");

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

                models.forEach((name, model) -> model.build(db));

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
