package com.botdiril.framework.sql.orm;

import javax.tools.*;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

import com.botdiril.framework.sql.util.SqlLogger;
import com.botdiril.framework.util.BotdirilInitializationException;

class ModelCompiler implements AutoCloseable
{
    private ModelClassLoader classLoader;

    private final Path javaSourcesDirectory;

    private final Map<String, Path> foundClasses;

    ModelCompiler(Path directory)
    {
        this.classLoader = new ModelClassLoader();

        if (!Files.isDirectory(directory))
            throw new IllegalArgumentException("Input path must be a directory.");

        this.javaSourcesDirectory = directory.resolve("java");

        if (!Files.isDirectory(this.javaSourcesDirectory))
            SqlLogger.instance.warn("Model directory `{}` does not contain a `java` directory.", directory);

        this.foundClasses = new HashMap<>();
    }

    void load(Consumer<Class<?>> classConsumer)
    {
        try
        {
            try (var tree = Files.walk(this.javaSourcesDirectory))
            {
                tree.filter(Files::isRegularFile)
                    .forEach(this::tryLoadFile);
            }

            foundClasses.keySet().forEach(this::compile);

            var classes = classLoader.getClasses();

            classes.forEach(classConsumer);
        }
        catch (Exception e)
        {
            throw new BotdirilInitializationException("Failed to load model:", e);
        }
    }

    @Override
    public void close()
    {
        SqlLogger.instance.info("Unloading model classes and the classloader.");

        classLoader = null;
        foundClasses.clear();
    }

    private void tryLoadFile(Path file)
    {
        var fileNameStr = file.toString();

        try
        {
            if (!fileNameStr.endsWith(".java"))
                return;

            var sourcePath = this.javaSourcesDirectory.relativize(file);
            var fs = FileSystems.getDefault();

            var sourcePathStr = sourcePath.toString();

            var className = sourcePathStr
                .replaceAll("\\.java$", "")
                .replace(fs.getSeparator(), ".");

            SqlLogger.instance.info("Compiling class '{}' from '{}'", className, fileNameStr);

            this.foundClasses.put(className, file);
        }
        catch (Exception e)
        {
            throw new BotdirilInitializationException("Failed to load class %s:".formatted(fileNameStr), e);
        }
    }

    private void compile(String className)
    {
        try
        {
            this.classLoader.loadClass(className);
        }
        catch (ClassNotFoundException clazzNotFoundExcept)
        {
            try
            {
                Function<Map.Entry<String, Path>, StringJavaFileObject> loader = entry -> loadObject(entry.getKey(), entry.getValue());
                compileFiles(foundClasses.entrySet().stream().map(loader).toList());
            }
            catch (ClassNotFoundException e)
            {
                throw new RuntimeException(e);
            }
        }
    }

    private static StringJavaFileObject loadObject(String className, Path path)
    {
        try
        {
            return new StringJavaFileObject(className, Files.readString(path));
        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

    private void compileFiles(List<StringJavaFileObject> files) throws ClassNotFoundException
    {
        var compiler = ToolProvider.getSystemJavaCompiler();

        var standardFileManager = compiler.getStandardFileManager(null, null, StandardCharsets.UTF_8);
        var fileManager = new ClassFileManager(standardFileManager);

        var compileTask = compiler.getTask(null, fileManager, null, null, null, files);
        compileTask.call();

        if (fileManager.objects.contains(null))
            throw new RuntimeException("Compile failed.");

        fileManager.objects.forEach(object -> this.classLoader.createClass(object.getClassName(), object.getBytes()));
    }

    private static final class JavaFileObject extends SimpleJavaFileObject
    {
        private final String className;
        private final ByteArrayOutputStream baos;

        JavaFileObject(String name, JavaFileObject.Kind kind)
        {
            super(URI.create("string:///" + name.replace('.', '/') + kind.extension), kind);
            this.baos = new ByteArrayOutputStream();
            this.className = name;
        }

        public String getClassName()
        {
            return this.className;
        }

        byte[] getBytes()
        {
            return this.baos.toByteArray();
        }

        @Override
        public OutputStream openOutputStream()
        {
            return this.baos;
        }
    }

    private static class ClassFileManager extends ForwardingJavaFileManager<StandardJavaFileManager>
    {
        private final List<JavaFileObject> objects;

        ClassFileManager(StandardJavaFileManager fileManager)
        {
            super(fileManager);
            this.objects = new ArrayList<>();
        }

        @Override
        public JavaFileObject getJavaFileForOutput(JavaFileManager.Location location, String className, JavaFileObject.Kind kind, FileObject sibling)
        {
            var object = new JavaFileObject(className, kind);
            objects.add(object);
            return object;
        }
    }

    private static class StringJavaFileObject extends SimpleJavaFileObject
    {
        private final String content;

        public StringJavaFileObject(String className, String content)
        {
            super(URI.create("string:///" + className.replace('.', '/') + JavaFileObject.Kind.SOURCE.extension), JavaFileObject.Kind.SOURCE);
            this.content = content;
        }

        @Override
        public String getCharContent(boolean ignoreEncodingErrors)
        {
            return this.content;
        }
    }

    static class ModelClassLoader extends ClassLoader
    {
        private final Map<String, Class<?>> classMap;

        private ModelClassLoader()
        {
            this.classMap = new HashMap<>();
        }

        @Override
        protected Class<?> findClass(String name) throws ClassNotFoundException
        {
            var clazz = this.classMap.get(name);

            if (clazz == null)
            {
                throw new ClassNotFoundException(String.format("Undefined class: '%s'", name));
            }

            return clazz;
        }

        public Collection<Class<?>> getClasses()
        {
            return Collections.unmodifiableCollection(this.classMap.values());
        }

        @Override
        protected Enumeration<URL> findResources(String name)
        {
            var resource = this.findResource(name);
            return resource == null ? Collections.emptyEnumeration() : Collections.enumeration(List.of(resource));
        }

        private void createClass(String name, byte[] data)
        {
            var clazz = this.defineClass(name, data, 0, data.length);
            this.resolveClass(clazz);
            this.classMap.put(name, clazz);
        }
    }
}
