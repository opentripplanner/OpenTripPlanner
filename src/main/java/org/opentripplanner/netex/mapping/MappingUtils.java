package org.opentripplanner.netex.mapping;

import java.util.function.Consumer;

/**
 * Collection of small utility functions for mapping NeTEx data to OTP objects.
 */
class MappingUtils {

    /** private constructor to prevent instantiation of utility class */
    private MappingUtils() {}

    /** Map given {@code element} if not {@code null} */
    static <T> void mapOptional(T element, Consumer<T> optionalMapping) {
        if(element != null) {
            optionalMapping.accept(element);
        }
    }
}
