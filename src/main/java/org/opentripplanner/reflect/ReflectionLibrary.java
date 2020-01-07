package org.opentripplanner.reflect;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;

public class ReflectionLibrary {

    private final StringBuilder sb = new StringBuilder();
    private final List<String> skipFields;


    private ReflectionLibrary(List<String> skipFields) {
        this.skipFields = skipFields;
    }

    /** Concatenate all fields and values of a Java object. */
    public static String dumpFields (Object object, String ... skipFields) {
        Class<?> clazz = object.getClass();
        ReflectionLibrary instance = new ReflectionLibrary(Arrays.asList(skipFields));
        instance.begin(clazz);
        instance.addFields("  ", clazz, object);
        return instance.end();
    }

    private void begin(Class<?> clazz) {
        sb.append("Summarizing ");
        sb.append(clazz.getSimpleName()).append(" {");
        sb.append('\n');
    }

    private String end() {
        sb.append("}");
        return sb.toString();
    }

    private void addFields(String indent, Class<?> clazz, Object object) {
        // Exit if the recursion is to deep; more than 8 levels down
        if(indent.length() > 16) { return; }
        // Add 2 spaces to the margin for each nested call to this method
        indent = indent + "  ";

        for (Field field : clazz.getFields()) {
            if(skipFields.contains(field.getName())) { continue; }
            int modifiers = field.getModifiers();
            if(Modifier.isStatic(modifiers)) { continue; }

            try {
                Class<?> fieldClass = field.getType();
                Object fieldValue = field.get(object);

                sb.append(indent);
                sb.append(field.getName());

                if(fieldValue != null && isOtpClass(fieldClass)) {
                    sb.append(" {\n");
                    addFields(indent, fieldClass, fieldValue);
                    sb.append(indent).append("}");
                }
                else {
                    String value = fieldValue == null ? "null" : fieldValue.toString();
                    sb.append(" = ");
                    sb.append(value);
                }
                sb.append('\n');
            }
            catch (IllegalAccessException ex) {
                sb.append(" = (non-public)\n");
            }
        }
    }

    private static boolean isOtpClass(Class<?> fieldClass) {
        return fieldClass.getName().startsWith("org.opentripplanner.");
    }
}
