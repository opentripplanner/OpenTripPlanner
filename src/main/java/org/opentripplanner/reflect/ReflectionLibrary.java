package org.opentripplanner.reflect;

import java.lang.reflect.Field;

public class ReflectionLibrary {

    /** Concatenate all fields and values of a Java object. */
    public static String dumpFields (Object object) {
        StringBuilder sb = new StringBuilder();
        Class<?> clazz = object.getClass();
        sb.append("Summarizing ");
        sb.append(clazz.getSimpleName());
        sb.append('\n');
        for (Field field : clazz.getFields()) {
            sb.append(field.getName());
            sb.append(" = ");
            try {
                Object fieldValue = field.get(object);
                String value = fieldValue == null ? "null" : fieldValue.toString();
                sb.append(value);
            } catch (IllegalAccessException ex) {
                sb.append("(non-public)");
            }
            sb.append('\n');
        }
        return sb.toString();
    }

}
