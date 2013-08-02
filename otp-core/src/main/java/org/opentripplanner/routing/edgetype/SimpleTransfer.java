package org.opentripplanner.routing.edgetype;

import lombok.Getter;

import org.opentripplanner.common.geometry.GeometryUtils;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStop;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.LineString;

/**
 * Represents a transfer between stops that does not take the street network into account.
 */
public class SimpleTransfer extends Edge {
    
    private static final long serialVersionUID = 1L;

    @Getter
    private double distance;
    
    public SimpleTransfer(TransitStop from, TransitStop to, double distance) {
        super(from, to);
        this.distance = distance;
    }

    @Override
    public State traverse(State s0) {
        // use transfer edges only to transfer 
        // otherwise they are used as shortcuts or break the itinerary generator 
//        if ( ! s0.isEverBoarded())
//            return null;
//        if (s0.getBackEdge() instanceof SimpleTransfer)
//            return null;
        RoutingRequest rr = s0.getOptions();
        double walkspeed = rr.getWalkSpeed();
        StateEditor se = s0.edit(this);
        se.setBackMode(TraverseMode.WALK);
        int time = (int) (distance / walkspeed); 
        se.incrementTimeInSeconds(time);
        se.incrementWeight(time * rr.walkReluctance);
        se.incrementWalkDistance(distance);
        return se.makeState();
    }

    @Override
    public LineString getGeometry() {
        TransitStop start = (TransitStop) fromv;
        TransitStop end = (TransitStop) tov;

        Coordinate c1 = new Coordinate(start.getLon(), start.getLat());
        Coordinate c2 = new Coordinate(end.getLon(), end.getLat());

        return GeometryUtils.getGeometryFactory().createLineString(new Coordinate[] {c1, c2});
    }

    @Override
    public String getName() {
        return "Simple Transfer";
    }

    @Override
    public double weightLowerBound(RoutingRequest rr) {
        int time = (int) (distance / rr.getWalkSpeed()); 
        return (time * rr.walkReluctance);
    }
    
}
