package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.model.P2;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.vertextype.SplitterVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TemporarySplitterVertex;
import org.opentripplanner.util.I18NString;

/**
 * This class is used to model a SemiPermanent StreetEdge that was non-destructively split from another StreetEdge.
 * These StreetEdges are typically used to model access to things such as bike rental stations that are dynamically
 * created from bike rental updaters and could be subsequently removed from the feed. These edges aid in providing
 * access only when needed to the desired bike rental stations and also allow for the isolated removal of these edges
 * when they are no longer needed.
 *
 * See https://github.com/opentripplanner/OpenTripPlanner/issues/2787 and associated comments for more details on what
 * inspired the creation of this class.
 */
public class SemiPermanentPartialStreetEdge extends PartialStreetEdge {
    private boolean bikeRentalOptionRequired = false;
    private boolean carRentalOptionRequired = false;
    private boolean vehicleRentalOptionRequired = false;

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
        if (
            (bikeRentalOptionRequired && !options.allowBikeRental) ||
                (carRentalOptionRequired && !options.allowCarRental) ||
                (vehicleRentalOptionRequired && !options.allowVehicleRental)
        ) {
            return null;
        }
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

    /**
     * Marks the edge as only traversable if a request is made that allows car rentals
     */
    public void setCarRentalOptionRequired() { carRentalOptionRequired = true; }

    /**
     * Marks the edge as only traversable if a request is made that allows vehicle rentals
     */
    public void setVehicleRentalOptionRequired() { vehicleRentalOptionRequired = true; }

    /**
     * Make sure that the only way to split a SemiPermanentPartialStreetEdge is with a TemporarySplitterVertex. The
     * whole point of SemiPermanentPartialStreetEdges is that they are only split once from a regular StreetEdge in
     * order to isolate access to a particular vertex that can be accessed from a StreetEdge such as a bike rental.
     */
    @Override public P2<StreetEdge> split(SplitterVertex splitterVertex, boolean destructive, boolean createSemiPermanentEdges) {
        if (!(splitterVertex instanceof TemporarySplitterVertex)) {
            throw new RuntimeException(
                "A split is being attempted on a SemiPermanentPartialStreetEdge using a vertex other than a "
                    + "TemporarySplitterVertex. Something is wrong!"
            );
        }
        return super.split(splitterVertex, destructive, createSemiPermanentEdges);
    }
}
