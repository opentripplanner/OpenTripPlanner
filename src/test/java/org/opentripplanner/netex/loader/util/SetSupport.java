package org.opentripplanner.netex.loader.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Small utility class with Set support methods for local tests
 */
class SetSupport {

    /**
     * Create a set of the given values.
     */
    @SafeVarargs
    static <E> Set<E> setOf(E ... values) {
        Set<E> set = new HashSet<>();
        Collections.addAll(set, values);
        return set;
    }

    /**
     * Create a list of the given values.
     * Alias for {@code Arrays.asList(values)}
     */
    @SafeVarargs
    static <E> List<E> listOf(E ... values) {
        return Arrays.asList(values);
    }

    /**
     * Create a new sorted list of the given values.
     */
    static <E extends Comparable> List<E> sort(Collection<E> values) {
        return values.stream().sorted().collect(Collectors.toList());
    }
}
