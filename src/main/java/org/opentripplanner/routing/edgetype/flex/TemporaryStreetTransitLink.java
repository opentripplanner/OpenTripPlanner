package org.opentripplanner.routing.edgetype.flex;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.edgetype.TemporaryEdge;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

public class TemporaryStreetTransitLink extends StreetTransitLink implements TemporaryEdge {


    public TemporaryStreetTransitLink(StreetVertex fromv, TransitStop tov) {
        super(fromv, tov);
    }

    public TemporaryStreetTransitLink(TransitStop fromv, StreetVertex tov) {
        super(fromv, tov);
    }

    public State traverse(State s0) {
        return super.traverse(s0);
    }

}
