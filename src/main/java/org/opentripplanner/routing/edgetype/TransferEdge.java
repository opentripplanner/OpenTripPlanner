package org.opentripplanner.routing.edgetype;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.vertextype.TransitStationStop;
import org.locationtech.jts.geom.LineString;
import java.util.Locale;

/**
 * A transfer directly between two stops without using the street network.
 *
 */
public class TransferEdge extends Edge {

    private static final long serialVersionUID = 1L;
    
    int time = 0;
    
    double distance;

    private LineString geometry = null;

    private boolean wheelchairAccessible = true;

    /**
     * @see Transfer(Vertex, Vertex, double, int)
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance) {
        this(fromv, tov, distance, (int) distance);
    }
    
    /**
     * Creates a new Transfer edge.
     * @param fromv     the Vertex where the transfer originates
     * @param tov       the Vertex where the transfer ends
     * @param distance  the distance in meters from the origin Vertex to the destination
     * @param time      the minimum time in seconds it takes to complete this transfer
     */
    public TransferEdge(TransitStationStop fromv, TransitStationStop tov, double distance, int time) {
        super(fromv, tov);
        this.distance = distance;
        this.time = time; 
    }

    public String getDirection() {
        return null;
    }

    public double getDistance() {
        return distance;
    }

    public LineString getGeometry() {
        return geometry;
    }

    public TraverseMode getMode() {
        return TraverseMode.WALK;
    }

    public String getName() {
        return "Transfer";
    }

    @Override
    public String getName(Locale locale) {
        //TODO: localize
        return this.getName();
    }

    public State traverse(State s0) {
        // only allow to use TransferEdges when already on a transit vehicle and not on a street level
        if (s0.getOptions().arriveBy) {
            if (!(s0.getBackEdge() instanceof PreBoardEdge)) return null;
        }
        else {
            if (!(s0.getBackEdge() instanceof PreAlightEdge)) return null;
        }
        if (s0.getOptions().wheelchairAccessible && !wheelchairAccessible) return null;
        if (this.getDistance() > s0.getOptions().maxTransferWalkDistance) return null;
        StateEditor s1 = s0.edit(this);
        s1.incrementTimeInSeconds(time);
        s1.incrementWeight(time);
        s1.setBackMode(TraverseMode.WALK);
        return s1.makeState();
    }

    public void setGeometry(LineString geometry) {
        this.geometry  = geometry;
    }

    public void setWheelchairAccessible(boolean wheelchairAccessible) {
        this.wheelchairAccessible = wheelchairAccessible;
    }

    public boolean isWheelchairAccessible() {
        return wheelchairAccessible;
    }

    @Override
    public String toString() {
        return "TransferEdge(" +
                getFromVertex() + " to " + getToVertex() + ")";
    }

}
