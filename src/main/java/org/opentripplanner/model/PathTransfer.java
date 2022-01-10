package org.opentripplanner.model;

import java.io.Serializable;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.opentripplanner.model.base.ToStringBuilder;
import org.opentripplanner.model.transfer.ConstrainedTransfer;
import org.opentripplanner.routing.api.request.StreetMode;
import org.opentripplanner.routing.graph.Edge;

/**
 * Represents a transfer between stops with the street network path attached to it.
 * <p>
 * Do not confuse this with {@link ConstrainedTransfer}.
 *
 * <p>
 * TODO Should we just store the NearbyStop as a field here, or even switch to using it instead
 *      where this class is used
 */
public class PathTransfer implements Serializable {
    private static final long serialVersionUID = 20200316L;

    public final StopLocation from;

    public final StopLocation to;

    private final double distanceMeters;

    private final List<Edge> edges;

    private final Set<StreetMode> modes;

    public PathTransfer(StopLocation from, StopLocation to, Set<StreetMode> mode, double distanceMeters, List<Edge> edges) {
        this.from = from;
        this.to = to;
        this.distanceMeters = distanceMeters;
        this.edges = edges;
        this.modes = mode;
    }

    public String getName() {
        return from + " => " + to;
    }

    public double getDistanceMeters() {
        return distanceMeters;
    }

    public List<Edge> getEdges() { return this.edges; }

    public PathTransfer copyWithDistanceMeters(double meters){
        return new PathTransfer(from, to, modes, meters, edges);
    }

    public PathTransfer copyWithAddedMode(StreetMode mode){
        var updatedModes = new HashSet<>(modes);
        updatedModes.add(mode);
        return new PathTransfer(from, to, updatedModes, distanceMeters, edges);
    }

    @Override
    public String toString() {
        return ToStringBuilder.of(getClass())
                .addObj("from", from)
                .addObj("to", to)
                .addNum("distance", distanceMeters)
                .addColSize("edges", edges)
                .toString();
    }
}
