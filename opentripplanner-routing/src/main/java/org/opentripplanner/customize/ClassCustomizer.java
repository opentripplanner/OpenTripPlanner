/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.customize;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtConstructor;
import javassist.CtNewConstructor;
import javassist.Modifier;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Bytecode;
import javassist.bytecode.ClassFile;
import javassist.bytecode.ConstPool;
import javassist.bytecode.DuplicateMemberException;
import javassist.bytecode.FieldInfo;
import javassist.bytecode.MethodInfo;
import javassist.bytecode.Opcode;
import javassist.util.proxy.FactoryHelper;

/**
 * This creates a subclass of a given class by adding a new interface to it, and allows users to add
 * any necessary fields
 * 
 * The intended use is to allow optimizations or extensions that require per-edge data to massage an
 * existing graph to meet their needs, without adding additional fields to the core. 
 * 
 * @author novalis
 * 
 */
public class ClassCustomizer {

    private ClassFile classFile;

    private CtClass ctClass;

    private File extraClassPath;

    public void setClassPath(File file) {
        this.extraClassPath = file;
    }

    /** 
     * 
     * @param iface The interface the new class should implement
     * @param oldlassName The class to be extended
     * @param newClassName the name of the new class to be created
     */
    public ClassCustomizer(Class<?> iface, String oldlassName, String newClassName) {

        try {
            ClassPool pool = ClassPool.getDefault();
            ctClass = pool.makeClass(newClassName);
            classFile = ctClass.getClassFile();
            classFile.setSuperclass(oldlassName);

            classFile.setName(newClassName);

            classFile.setInterfaces(new String[] { iface.getName() });

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Adds a new field of type double to the customized class */
    public void addDoubleField(String fieldName) {
        // FIXME: this should support default values but does not

        ClassFile classFile = ctClass.getClassFile();
        ConstPool constPool = classFile.getConstPool();
        try {
            // add field
            FieldInfo fieldInfo = new FieldInfo(constPool, fieldName, "D");
            classFile.addField(fieldInfo);

            CtConstructor ctor = CtNewConstructor.defaultConstructor(ctClass);
            ctClass.addConstructor(ctor);

            addDoubleSetter(classFile, fieldName);
            addDoubleGetter(classFile, fieldName);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Writes the class file to the classpath and returns a class object */
    public Class<?> saveClass() {
        ClassFile classFile = ctClass.getClassFile();

        try {
            if (!extraClassPath.exists()) {
                extraClassPath.mkdirs();
            }
            FactoryHelper.writeFile(classFile, extraClassPath.getPath());
            ClassLoader loader = getClass().getClassLoader();

            Class<?> cls = FactoryHelper.toClass(classFile, loader);
            return cls;
            // load class
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Creates a clone of original but with the class NewClass (which extends original's class) */
    public static <T> T reclass(T original, Class<? extends T> newClass) {
        Class<?> origClass = original.getClass();

        T newObj;
        try {
            Constructor<? extends T> ctor = newClass.getConstructor();
            newObj = ctor.newInstance();

            while (origClass != null) {
                Field[] fields = origClass.getDeclaredFields();
                for (Field field : fields) {
                    field.setAccessible(true);
                    int modifiers = field.getModifiers();
                    if (Modifier.isStatic(modifiers)) {
                        continue;
                    }
                    Object value = field.get(original);
                    field.set(newObj, value);
                }
                origClass = origClass.getSuperclass();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return newObj;
    }

    /**
     * capitalize the first letter of the string
     * 
     * @param str
     * @return
     */
    private String ucfirst(String str) {
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Add a simple getter with signature "double getFoo()" to the class, which simply returns the value of the 
     * field fieldName 
     * @param ctClass
     * @param classFile
     * @param fieldName
     * @throws DuplicateMemberException
     */
    private void addDoubleGetter(ClassFile classFile, String fieldName)
            throws DuplicateMemberException {
        ConstPool constPool = classFile.getConstPool();

        // double getFoo()
        MethodInfo getter = new MethodInfo(constPool, "get" + ucfirst(fieldName), "()D");

        Bytecode code = new Bytecode(constPool, 2, 1);

        // load this
        code.addAload(0);

        code.addGetfield(ctClass, fieldName, "D");

        // return with value
        code.addOpcode(Opcode.DRETURN);
        getter.setCodeAttribute(code.toCodeAttribute());

        getter.setAccessFlags(AccessFlag.PUBLIC);
        classFile.addMethod(getter);
    }

    private void addDoubleSetter(ClassFile classFile, String fieldName)
            throws DuplicateMemberException {
        ConstPool constPool = classFile.getConstPool();

        // void setFoo(double)
        MethodInfo setter = new MethodInfo(constPool, "set" + ucfirst(fieldName), "(D)V");
        Bytecode code = new Bytecode(constPool, 3, 3);

        // load this
        code.addAload(0);

        // load param
        code.addDload(1);
        code.addPutfield(ctClass, fieldName, "D");

        code.addOpcode(Opcode.RETURN);

        setter.setCodeAttribute(code.toCodeAttribute());
        setter.setAccessFlags(AccessFlag.PUBLIC);
        classFile.addMethod(setter);

    }
}
