package org.opentripplanner.netex.loader.util;

import org.rutebanken.netex.model.EntityInVersionStructure;
import org.rutebanken.netex.model.EntityStructure;

/**
 * Create some entities. The classes under test is generic classes so we
 * have used american acters with a political career as sample data - just for the fun of it.
 * <p/>
 * This class extends {@link EntityInVersionStructure} because some of the classes under
 * test only accepts this tye or the parent class {@link EntityStructure}.
 */
class E extends EntityInVersionStructure implements Comparable<E> {
    /** President and actor, version 1 */
    static final E REAGAN = new E(11, "Reagan", 1);
    /** President and actor, version 2 */
    static final E REAGAN_2 = new E(11, "Reagan", 2);
    /** Governor and actor */
    static final E SCHWARZENEGGER = new E(12,"Schwarzenegger", 1);
    /** Mayor and actor */
    static final E EASTWOOD = new E(13,"Eastwood", 1);

    private final String name;

    private E(int id, String name, int version) {
        this.name = name;
        setId("id-" + id);
        setVersion(Integer.toString(version));
    }

    @Override public String toString() {
        return String.format("E(%s, %s)", name, getVersion());
    }

    @Override public int compareTo(E o) {
        int res = name.compareTo(o.name) ;
        return res == 0 ? version.compareTo(o.version) : res;
    }
}
