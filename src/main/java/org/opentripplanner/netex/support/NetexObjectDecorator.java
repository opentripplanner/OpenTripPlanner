package org.opentripplanner.netex.support;

import org.rutebanken.netex.model.VersionOfObjectRefStructure;
import org.slf4j.Logger;

import javax.validation.constraints.NotNull;
import java.util.function.Consumer;

/**
 * Collection of small utility functions for mapping NeTEx data to OTP objects.
 */
public class NetexObjectDecorator {

    /** private constructor to prevent instantiation of utility class */
    private NetexObjectDecorator() {}

    /**
     * Handle given {@code element} if not {@code null}. This method reduce
     * the code from:
     * <pre>
     * if(entity.getProperty() != null) {
     *     SubType x = entity.getProperty();
     *     [do something with x]
     * }
     * </pre>
     * to:
     * <pre>
     * withOptional(entity.getProperty(), x -> {
     *     [do something with x]
     * });
     * </pre>
     */
    public static <T> void withOptional(T element, Consumer<T> optionalHandler) {
        if(element != null) {
            optionalHandler.accept(element);
        }
    }

    public static void foo() {

    }

    /**
     * Use this method to log unmapped entities. The entity mys be part of the supported profile,
     * or not supported at all.
     * <p/>
     * Consider implementing the mapper.
     *
     * @param log the logger to use, passing the logger in as an argument make sure the log event
     *            get the right scope - this class is just a utility class, or the messenger.
     * @param ref the unexpected reference to an unmapped object.
     */
    public static void logUnmappedEntityRef(Logger log, @NotNull VersionOfObjectRefStructure ref) {
        log.warn("Unexpected entity {} in NeTEx import. The entity is ignored.", ref);
    }
}
