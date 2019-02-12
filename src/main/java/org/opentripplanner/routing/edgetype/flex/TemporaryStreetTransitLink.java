package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TemporaryStreetTransitLink extends StreetTransitLink implements TemporaryEdge {


    public TemporaryStreetTransitLink(StreetVertex fromv, TransitStop tov, boolean wheelchairAccessible) {
        super(fromv, tov, wheelchairAccessible);
    }

    public TemporaryStreetTransitLink(TransitStop fromv, StreetVertex tov, boolean wheelchairAccessible) {
        super(fromv, tov, wheelchairAccessible);
    }

    public State traverse(State s0) {
        return super.traverse(s0);
    }

}
