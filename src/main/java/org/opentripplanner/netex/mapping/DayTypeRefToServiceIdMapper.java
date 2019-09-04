package org.opentripplanner.netex.mapping;

import java.util.Collection;

/**
 * The concept ServiceId does not exist in NeTEx. To generate a Service Id a simple
 * strategy of concatenating the dayTypes together is used.
 * <p/>
 * Dont parse the service date to get dayTypeRefs, the reason way we keep the refs is
 * to allow the ServiceId to be human readable.
 */
public class DayTypeRefToServiceIdMapper {
    private static final char SEP = '+';

    /** private constructor to prevent instantiation of utility class */
    private DayTypeRefToServiceIdMapper() {
    }

    /**
     * Generate a new ServiceId based on the dayTypeRefs. This method should be deterministic,
     * tha same id should be generated every time the same set if refs is passed inn and if
     * two different set should always generate diffrent ServiceIds. The order of the elements
     * in the input set should not matter and duplicate elements in the set ignored.
     */
    public static String generateServiceId(Collection<String> dayTypeRefs) {
        return dayTypeRefs.stream().distinct().sorted().reduce((s,t) -> s + SEP + t).orElse(
                null
        );
    }
}
