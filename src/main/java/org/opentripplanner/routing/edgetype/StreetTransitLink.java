package org.opentripplanner.routing.edgetype;

import org.opentripplanner.model.Trip;
import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LineString;

import java.util.Locale;

/** 
 * This represents the connection between a street vertex and a transit vertex
 * where going from the street to the vehicle is immediate -- such as at a 
 * curbside bus stop.
 */
public class StreetTransitLink extends Edge {

    private static final long serialVersionUID = -3311099256178798981L;
    static final int STL_TRAVERSE_COST = 1;

    private boolean wheelchairAccessible;

    private TransitStop transitStop;

    public StreetTransitLink(StreetVertex fromv, TransitStop tov, boolean wheelchairAccessible) {
    	super(fromv, tov);
    	transitStop = tov;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public StreetTransitLink(TransitStop fromv, StreetVertex tov, boolean wheelchairAccessible) {
        super(fromv, tov);
        transitStop = fromv;
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return 0;
    }

    public LineString getGeometry() {
        Coordinate[] coordinates = new Coordinate[] { fromv.getCoordinate(), tov.getCoordinate()};
        return GeometryUtils.getGeometryFactory().createLineString(coordinates);
    }

    public TraverseMode getMode() {
        return TraverseMode.LEG_SWITCH;
    }

    public String getName() {
        return "street transit link";
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    public State traverse(State s0) {

        // Forbid taking shortcuts composed of two street-transit links associated with the same stop in a row. Also
        // avoids spurious leg transitions. As noted in https://github.com/opentripplanner/OpenTripPlanner/issues/2815,
        // it is possible that two stops can have the same GPS coordinate thus creating a possibility for a
        // legitimate StreetTransitLink > StreetTransitLink sequence, so only forbid two StreetTransitLinks to be taken
        // if they are for the same stop.
        if (
            s0.backEdge instanceof StreetTransitLink &&
                ((StreetTransitLink) s0.backEdge).transitStop == this.transitStop
        ) {
            return null;
        }

        // Do not re-enter the street network following a transfer.
        if (s0.backEdge instanceof SimpleTransfer) {
            return null;
        }

        // Do not get off at a real stop when on call-n-ride (force a transfer instead).
        if (s0.isLastBoardAlightDeviated() && !(transitStop.checkCallAndRideStreetLinkOk(s0))) {
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
        if (s0.getNonTransitMode() == TraverseMode.CAR && !req.enterStationsWithCar) {
            if (req.kissAndRide && !s0.isCarParked()) {
                s1.setCarParked(true);
            } else {
                return null;
            }
        }
        s1.incrementTimeInSeconds(transitStop.getStreetToStopTime() + STL_TRAVERSE_COST);
        s1.incrementWeight(STL_TRAVERSE_COST + transitStop.getStreetToStopTime());
        s1.setBackMode(TraverseMode.LEG_SWITCH);
        return s1.makeState();
    }

    public State optimisticTraverse(State s0) {
        StateEditor s1 = s0.edit(this);
        s1.incrementWeight(STL_TRAVERSE_COST);
        s1.setBackMode(TraverseMode.LEG_SWITCH);
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

    public Trip getTrip() {
        return null;
    }

    public boolean isRoundabout() {
        return false;
    }

    public String toString() {
        return "StreetTransitLink(" + fromv + " -> " + tov + ")";
    }

}
