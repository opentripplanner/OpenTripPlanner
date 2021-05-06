package org.opentripplanner.model;

import java.io.Serializable;
import java.util.List;
import org.opentripplanner.routing.graph.Edge;

/**
 * Represents a transfer between stops that does not take the street network into account.
 * <p>
 * Do not confuse this with {@link org.opentripplanner.model.transfer.Transfer}.
 *
 * <p>
 * TODO these should really have a set of valid modes in case bike vs. walk transfers are different
 * TODO Should we just store the NearbyStop as a field here, or even switch to using it instead
 * where this class is used
 */
public class SimpleTransfer implements Serializable {
    private static final long serialVersionUID = 20200316L;
    public final StopLocation from;
    public final StopLocation to;

    private final double effectiveWalkDistance;
    private final int distanceIndependentTime;

    private final List<Edge> edges;

    public SimpleTransfer(StopLocation from, StopLocation to, double effectiveWalkDistance, int distanceIndependentTime, List<Edge> edges) {
        this.from = from;
        this.to = to;
        this.effectiveWalkDistance = effectiveWalkDistance;
        this.distanceIndependentTime = distanceIndependentTime;
        this.edges = edges;
    }

    public String getName() {
        return from + " => " + to;
    }

    public double getDistanceMeters() {
        return edges.stream().mapToDouble(Edge::getDistanceMeters).sum();
    }

    /**
     * The distance to walk adjusted for elevation and obstacles. This is used together
     * with the walking speed to find the actual walking transfer time. This plus
     * {@link #getDistanceIndependentTime()} is used to calculate the actual-transfer-time
     * given a walking speed.
     * <p>
     * Unit: meters. Default: 0.
     * @see Edge#getEffectiveWalkDistance()
     */
    public double getEffectiveWalkDistance(){
    	return this.effectiveWalkDistance;
    }

    /**
     * This is the transfer time(duration) spent NOT moving like time in in elevators, escalators
     * and waiting on read light when crossing a street. This is used together with
     * {@link #getEffectiveWalkDistance()} to calculate the actual-transfer-time.
     * <p>
     * Unit: seconds. Default: 0.
     * @see Edge#getDistanceIndependentTime()
     */
    public int getDistanceIndependentTime() {
        return distanceIndependentTime;
    }

    public List<Edge> getEdges() { return this.edges; }

    @Override
    public String toString() {
        return "SimpleTransfer " + getName();
    }
}
