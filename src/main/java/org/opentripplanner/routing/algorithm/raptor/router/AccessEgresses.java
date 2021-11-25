package org.opentripplanner.routing.algorithm.raptor.router;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;

class AccessEgresses {

    private final Collection<AccessEgress> accesses;
    private final Collection<AccessEgress> egresses;

    AccessEgresses(
            Collection<AccessEgress> accesses,
            Collection<AccessEgress> egresses
    ) {
        this.accesses = accesses;
        this.egresses = egresses;
    }

    Collection<AccessEgress> getAccesses() {
        return accesses;
    }

    Collection<AccessEgress> getEgresses() {
        return egresses;
    }
}
