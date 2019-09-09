package org.opentripplanner.routing.vertextype;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StopVertex extends Vertex {

    private static final Logger LOG = LoggerFactory.getLogger(StopVertex.class);

    // Do we actually need a set of modes for each stop?
    // It's nice to have for the index web API but can be generated on demand.
    private TraverseModeSet modes = new TraverseModeSet();

    private static final long serialVersionUID = 1L;

    private boolean wheelchairEntrance;

    private boolean isEntrance;

    private Stop stop;

    /**
     * For stops that are deep underground, there is a time cost to entering and exiting the stop;
     * all stops are assumed to be at street level unless we have configuration to the contrary
     */
    private int streetToStopTime = 0;

    public StopVertex(Graph graph, Stop stop) {
        super(graph, GtfsLibrary.convertIdToString(stop.getId()), stop.getLon(), stop.getLat());
        this.stop = stop;
        this.wheelchairEntrance = stop.getWheelchairBoarding() != WheelChairBoarding.NOT_POSSIBLE;
        isEntrance = false; // Entrance not supported in current otp model
        //Adds this vertex into graph envelope so that we don't need to loop over all vertices
        graph.expandToInclude(stop.getLon(), stop.getLat());
    }

    public boolean hasWheelchairEntrance() {
        return wheelchairEntrance;
    }

    public boolean isEntrance() {
        return isEntrance;
    }

    public boolean hasEntrances() {
        for (Edge e : this.getOutgoing()) {
            if (e instanceof PathwayEdge) {
                return true;
            }
        }
        for (Edge e : this.getIncoming()) {
            if (e instanceof PathwayEdge) {
                return true;
            }
        }
        return false;
    }

    public int getStreetToStopTime() {
        return streetToStopTime;
    }

    public void setStreetToStopTime(int streetToStopTime) {
        this.streetToStopTime = streetToStopTime;
        LOG.debug("Stop {} access time from street level set to {}", this, streetToStopTime);
    }

    public TraverseModeSet getModes() {
        return modes;
    }

    public void addMode(TraverseMode mode) {
        modes.setMode(mode, true);
    }
    
    public boolean isStreetLinkable() {
        return isEntrance() || !hasEntrances();
    }

    public Stop getStop() {
            return this.stop;
    }
}
