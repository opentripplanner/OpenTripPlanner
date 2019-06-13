package org.opentripplanner.netex.support;

import org.rutebanken.netex.model.EntityInVersionStructure;

import java.util.Collection;
import java.util.Comparator;

import static java.util.Comparator.comparingInt;

/**
 * Utility class to help working with versioned NeTEx element.
 * <p/>
 * This class implements <em>Norwegian profile</em> specific rules.
 */
public class NetexVersionHelper {

    /**
     * private constructor to prevent instantiation of utility class
     */
    private NetexVersionHelper() { }

    /**
     * According to the <b>Norwegian Netex profile</b> the version number must be a
     * positive increasing integer. A bigger value indicate a later version.
     */
    public static int versionOf(EntityInVersionStructure e) {
        return Integer.parseInt(e.getVersion());
    }

    /**
     * Return the latest (maximum) version number for the given {@code list} of elements.
     * If no elements exist in the collection {@code -1} is returned.
     */
    public static int latestVersionIn(Collection<? extends EntityInVersionStructure> list) {
        return list.stream().mapToInt(NetexVersionHelper::versionOf).max().orElse(-1);
    }

    /**
     * Return the element with the latest (maximum) version for a given {@code list} of elements.
     * If no elements exist in the collection {@code null} is returned.
     */
    public static <T extends EntityInVersionStructure> T latestVersionedElementIn(Collection<T> list) {
        return list.stream().max(comparingVersion()).orElse(null);
    }

    /**
     * Return a comparator to compare {@link EntityInVersionStructure} elements by <b>version</b>.
     */
    public static <T extends EntityInVersionStructure> Comparator<T> comparingVersion() {
        return comparingInt(NetexVersionHelper::versionOf);
    }
}
