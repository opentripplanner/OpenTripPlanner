package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.util.I18NString;

/**
 * This class is used to model a SemiPermanent StreetEdge that was non-destructively split from another StreetEdge.
 * These StreetEdges are typically used to model access to things such as bike rental stations that are dynamically
 * created from bike rental updaters and could be subsequently removed from the feed. These edges aid in providing
 * access only when needed to the desired bike rental stations and also allow for the isolated removal of these edges
 * when they are no longer needed.
 */
public class SemiPermanentPartialStreetEdge extends PartialStreetEdge {
    private boolean bikeRentalOptionRequired = false;

    public SemiPermanentPartialStreetEdge(
        StreetEdge streetEdge,
        StreetVertex v1,
        StreetVertex v2,
        LineString geometry,
        I18NString name
    ) {
        super(streetEdge, v1, v2, geometry, name, 0);
    }

    /**
     * There can often times be requests that do not need to traverse this edge, such as requests that don't want to
     * use a bike rental. In those cases, return null to avoid exploring this edge and any connected vertices further.
     */
    @Override public State traverse(State s0) {
        RoutingRequest options = s0.getOptions();
        if (bikeRentalOptionRequired && !options.allowBikeRental) return null;
        return super.traverse(s0);
    }

    @Override
    public String toString() {
        return "SemiPermanentPartialStreetEdge(" + this.getName() + ", " + this.getFromVertex() + " -> "
            + this.getToVertex() + " length=" + this.getDistance() + " carSpeed="
            + this.getCarSpeed() + " parentEdge=" + parentEdge + ")";
    }

    /**
     * Marks the edge as only traversable if a request is made that allows bike rentals
     */
    public void setBikeRentalOptionRequired() {
        bikeRentalOptionRequired = true;
    }
}
