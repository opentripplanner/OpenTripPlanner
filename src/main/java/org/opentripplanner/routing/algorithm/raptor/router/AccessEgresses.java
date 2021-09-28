package org.opentripplanner.routing.algorithm.raptor.router;

import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptor.transit.AccessEgress;

public class AccessEgresses {

    private final Collection<AccessEgress> accesses;
    private final Collection<AccessEgress> egresses;

    public AccessEgresses(
            Collection<AccessEgress> accesses,
            Collection<AccessEgress> egresses
    ) {
        this.accesses = accesses;
        this.egresses = egresses;
    }

    public Collection<AccessEgress> getAccesses() {
        return accesses;
    }

    public Collection<AccessEgress> getEgresses() {
        return egresses;
    }
}
