package org.opentripplanner.routing.edgetype;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.api.request.RoutingRequest;
import org.opentripplanner.routing.core.CarPickupState;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitEntranceVertex;

import java.util.Locale;

/** 
 * This represents the connection between a street vertex and a transit vertex belonging the street
 * network.
 */
public class TransitEntranceLink extends Edge {

    private static final long serialVersionUID = -3311099256178798981L;
    static final int TEL_TRAVERSE_COST = 1;

    private boolean wheelchairAccessible;

    private TransitEntranceVertex entranceVertex;

    public TransitEntranceLink(StreetVertex fromv, TransitEntranceVertex tov, boolean wheelchairAccessible) {
        super(fromv, tov);
        entranceVertex = tov;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public TransitEntranceLink(TransitEntranceVertex fromv, StreetVertex tov, boolean wheelchairAccessible) {
        super(fromv, tov);
        entranceVertex = fromv;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate()};
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    public String getName() {
        return entranceVertex.getName();
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return entranceVertex.getName();
    }

    public State traverse(State s0) {
        // Forbid taking shortcuts composed of two street-transit links associated with the same entrance in a row.
        // As noted in https://github.com/opentripplanner/OpenTripPlanner/issues/2815, it is
        // possible that two stops can have the same GPS coordinate thus creating a possibility for
        // a legitimate TransitEntranceLink > TransitEntranceLink sequence, so only forbid two
        // TransitEntranceLinks to be taken if they are for the same entrance.
        if (
            s0.backEdge instanceof TransitEntranceLink &&
                ((TransitEntranceLink) s0.backEdge).entranceVertex == this.entranceVertex
        ) {
            return null;
        }

        RoutingRequest req = s0.getOptions();
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) {
            return null;
        }

        if (s0.getOptions().bikeParkAndRide && !s0.isBikeParked()) {
            // Forbid taking your own bike in the station if bike P+R activated.
            return null;
        }
        if (s0.isBikeRenting()) {
            // Forbid taking a rented bike on any transit.
            // TODO Check this condition, does this always make sense?
            return null;
        }

        // Do not check here whether any transit modes are selected. A check for the presence of
        // transit modes will instead be done in the following PreBoard edge.
        // This allows searching for nearby transit stops using walk-only options.
        StateEditor s1 = s0.edit(this);

        /* Only enter stations in CAR mode if parking is not required (kiss and ride) */
        /* Note that in arriveBy searches this is double-traversing link edges to fork the state into both WALK and CAR mode. This is an insane hack. */
        if (s0.getNonTransitMode() == TraverseMode.CAR) {
            if (req.carPickup && s0.getCarPickupState().equals(CarPickupState.IN_CAR)) {
                s1.setTaxiState(CarPickupState.WALK_FROM_DROP_OFF);
            } else {
                return null;
            }
        }

        s1.incrementTimeInSeconds(TEL_TRAVERSE_COST);
        s1.incrementWeight(TEL_TRAVERSE_COST);
        return s1.makeState();
    }

    public State optimisticTraverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(TEL_TRAVERSE_COST);
        return s1.makeState();
    }
    
    // anecdotally, the lower bound search is about 2x faster when you don't reach stops
    // and therefore don't even consider boarding
    @Override
    public double weightLowerBound(RoutingRequest options) {
        return options.transitAllowed() ? 0 : Double.POSITIVE_INFINITY;
    }

    public Vertex getFromVertex() {
        return fromv;
    }

    public Vertex getToVertex() {
        return tov;
    }

    public boolean isRoundabout() {
        return false;
    }

    public String toString() {
        return "TransitEntranceLink(" + fromv + " -> " + tov + ")";
    }


}
