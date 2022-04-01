package com.botdiril.framework.sql.orm.asm;

import org.objectweb.asm.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.botdiril.framework.sql.orm.Model;
import com.botdiril.framework.sql.orm.ModelColumn;
import com.botdiril.framework.sql.orm.column.ColumnInfo;

public class TableInfoGenerator
{
    private static final String OUTPUT_PACKAGE = "com/botdiril/tableinfo/";

    public record GeneratedClass(String name, String path, byte[] data)
    {

    }

    public static List<GeneratedClass> generate(Model model)
    {
        var outputClasses = new ArrayList<GeneratedClass>();

        var schemaClass = model.getSchemaClass();

        for (var modelTable : model.getTables())
        {
            var klass = modelTable.getTableClass();

            var classSimpleName = klass.getSimpleName() + "Data";

            var createdType = Type.getObjectType(OUTPUT_PACKAGE + classSimpleName);

            ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES + ClassWriter.COMPUTE_MAXS);

            cw.visit(Opcodes.V17,
                Opcodes.ACC_RECORD + Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL + Opcodes.ACC_SUPER,
                createdType.getInternalName(),
                null,
                Type.getInternalName(Record.class),
                null);

            var columns = modelTable.getColumns();

            for (var column : columns)
            {
                var colInfo = column.getInfo();

                var componentName = colInfo.name();

                var rw = cw.visitRecordComponent(componentName, Type.getDescriptor(colInfo.javaType()), null);
                rw.visitEnd();
            }

            generateRecordMethods(cw, createdType, columns);

            generateStaticInfo(cw, createdType, schemaClass);

            cw.visitEnd();

            var classData = new GeneratedClass(createdType.getClassName(), OUTPUT_PACKAGE, cw.toByteArray());

            outputClasses.add(classData);
        }

        return outputClasses;
    }

    private static void generateRecordMethods(ClassWriter cw, Type createdType, Collection<ModelColumn<?>> columns)
    {
        for (var col : columns)
        {
            var info = col.getInfo();

            var type = Type.getType(info.javaType());

            var fw = cw.visitField(Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL,
                info.name(),
                type.getDescriptor(),
                null,
                null);
            fw.visitEnd();
        }


        var colTypes = columns.stream()
                              .map(ModelColumn::getInfo)
                              .map(ColumnInfo::javaType)
                              .map(Type::getType)
                              .toList();

        var cmw = cw.visitMethod(Opcodes.ACC_PUBLIC,
            "<init>",
            Type.getMethodDescriptor(Type.getType(void.class), colTypes.toArray(new Type[0])),
            null,
            null);

        columns.stream().map(ModelColumn::getInfo)
               .map(ColumnInfo::name)
               .forEach(colName -> cmw.visitParameter(colName, 0));

        cmw.visitCode();
        cmw.visitVarInsn(Opcodes.ALOAD, 0);
        cmw.visitMethodInsn(Opcodes.INVOKESPECIAL,
            Type.getInternalName(Record.class),
            "<init>",
            Type.getMethodDescriptor(Type.getType(void.class)),
            false);

        var size = 1;
        for (var col : columns)
        {
            var info = col.getInfo();

            var type = Type.getType(info.javaType());
            cmw.visitVarInsn(Opcodes.ALOAD, 0);
            cmw.visitVarInsn(type.getOpcode(Opcodes.ILOAD), size);
            cmw.visitFieldInsn(Opcodes.PUTFIELD, createdType.getInternalName(), info.name(), type.getDescriptor());

            size += type.getSize();
        }

        cmw.visitInsn(Opcodes.RETURN);
        cmw.visitMaxs(Type.getType(Object.class).getSize() * 3, size);
        cmw.visitEnd();

        for (var col : columns)
        {
            var info = col.getInfo();

            var type = Type.getType(info.javaType());

            var gmw = cw.visitMethod(Opcodes.ACC_PUBLIC,
                info.name(),
                Type.getMethodDescriptor(type),
                null,
                null
            );
            gmw.visitCode();
            gmw.visitVarInsn(Opcodes.ALOAD, 0);
            gmw.visitFieldInsn(Opcodes.GETFIELD, createdType.getInternalName(), info.name(), type.getDescriptor());
            gmw.visitInsn(type.getOpcode(Opcodes.IRETURN));
            gmw.visitMaxs(type.getSize(), 1);
            gmw.visitEnd();
        }

        var handle = new Handle(Opcodes.H_INVOKESTATIC,
            "java/lang/runtime/ObjectMethods",
            "bootstrap",
            "(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/TypeDescriptor;Ljava/lang/Class;Ljava/lang/String;[Ljava/lang/invoke/MethodHandle;)Ljava/lang/Object;",
            false);

        var argList = new ArrayList<>();
        argList.add(createdType);
        argList.add(columns.stream().map(ModelColumn::getName).collect(Collectors.joining(";")));

        columns.stream()
               .map(column -> {
                   var info = column.getInfo();
                   return new Handle(Opcodes.H_GETFIELD, createdType.getInternalName(), column.getName(), Type.getDescriptor(info.javaType()), false);
               })
               .forEach(argList::add);

        var args = argList.toArray();

        var mwToString = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
            "toString",
            Type.getMethodDescriptor(Type.getType(String.class)),
            null,
            null);

        mwToString.visitCode();
        mwToString.visitVarInsn(Opcodes.ALOAD, 0);
        mwToString.visitInvokeDynamicInsn(
            "toString",
            Type.getMethodDescriptor(Type.getType(String.class), createdType),
            handle,
            args);
        mwToString.visitInsn(Opcodes.ARETURN);
        mwToString.visitMaxs(Type.getType(Object.class).getSize(), 0);
        mwToString.visitEnd();

        var mwHashCode = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
            "hashCode",
            Type.getMethodDescriptor(Type.getType(int.class)),
            null,
            null);

        mwHashCode.visitCode();
        mwHashCode.visitVarInsn(Opcodes.ALOAD, 0);
        mwHashCode.visitInvokeDynamicInsn(
            "hashCode",
            Type.getMethodDescriptor(Type.getType(int.class), createdType),
            handle,
            args);
        mwHashCode.visitInsn(Opcodes.IRETURN);
        mwHashCode.visitMaxs(Type.getType(Object.class).getSize(), 0);
        mwHashCode.visitEnd();

        var mwEquals = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL,
            "equals",
            Type.getMethodDescriptor(Type.getType(boolean.class), Type.getType(Object.class)),
            null,
            null);

        mwEquals.visitCode();
        mwEquals.visitVarInsn(Opcodes.ALOAD, 0);
        mwEquals.visitVarInsn(Opcodes.ALOAD, 1);
        mwEquals.visitInvokeDynamicInsn(
            "equals",
            Type.getMethodDescriptor(Type.getType(boolean.class), createdType, Type.getType(Object.class)),
            handle,
            args);
        mwEquals.visitInsn(Opcodes.IRETURN);
        mwEquals.visitMaxs(Type.getType(Object.class).getSize() * 2, 0);
        mwEquals.visitEnd();
    }

    private static void generateStaticInfo(ClassWriter cw, Type createdType, Class<?> schemaClass)
    {
        var schemaClassFld = cw.visitField(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC + Opcodes.ACC_FINAL,
            "SCHEMA_CLASS",
            Type.getDescriptor(Class.class),
            null,
            null);
        schemaClassFld.visitEnd();

        var mw = cw.visitMethod(Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC,
            "<clinit>",
            Type.getMethodDescriptor(Type.getType(void.class)),
            null,
            null);

        mw.visitCode();
        var start = new Label();
        var end = new Label();
        var handler = new Label();
        mw.visitTryCatchBlock(start, end, handler, Type.getInternalName(ClassNotFoundException.class));
        mw.visitLabel(start);
        mw.visitLdcInsn(schemaClass.getName());
        mw.visitMethodInsn(Opcodes.INVOKESTATIC,
            Type.getInternalName(Class.class),
            "forName",
            Type.getMethodDescriptor(Type.getType(Class.class), Type.getType(String.class)),
            false);
        mw.visitFieldInsn(Opcodes.PUTSTATIC, createdType.getInternalName(), "SCHEMA_CLASS", Type.getDescriptor(Class.class));
        mw.visitJumpInsn(Opcodes.GOTO, end);
        mw.visitLabel(handler);
        mw.visitFrame(Opcodes.F_SAME1,
            1, new Object[] { Type.getInternalName(ClassNotFoundException.class) },
            1, new Object[] { Type.getInternalName(RuntimeException.class) });
        mw.visitVarInsn(Opcodes.ASTORE, 0);
        mw.visitTypeInsn(Opcodes.NEW, Type.getInternalName(RuntimeException.class));
        mw.visitInsn(Opcodes.DUP);
        mw.visitVarInsn(Opcodes.ALOAD, 0);
        mw.visitMethodInsn(Opcodes.INVOKESPECIAL,
            Type.getInternalName(RuntimeException.class),
            "<init>",
            Type.getMethodDescriptor(Type.getType(void.class), Type.getType(Throwable.class)),
            false);
        mw.visitInsn(Opcodes.ATHROW);
        mw.visitLabel(end);
        mw.visitLocalVariable("e", Type.getDescriptor(RuntimeException.class), null, handler, end, 0);
        mw.visitInsn(Opcodes.RETURN);
        mw.visitMaxs(Type.getType(Object.class).getSize() * 3, Type.getType(Object.class).getSize());
        mw.visitEnd();
    }
}
