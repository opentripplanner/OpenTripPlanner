package org.opentripplanner.routing.vertextype;

import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.model.StationElement;
import org.opentripplanner.model.Stop;
import org.opentripplanner.model.WheelChairBoarding;
import org.opentripplanner.routing.RoutingService;
import org.opentripplanner.routing.edgetype.PathwayEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.model.TransitMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;


public class TransitStopVertex extends Vertex {

    private static final Logger LOG = LoggerFactory.getLogger(TransitStopVertex.class);

    // Do we actually need a set of modes for each stop?
    // It's nice to have for the index web API but can be generated on demand.
    private final Set<TransitMode> modes;

    private static final long serialVersionUID = 1L;

    private boolean wheelchairEntrance;

    private Stop stop;

    /**
     * For stops that are deep underground, there is a time cost to entering and exiting the stop;
     * all stops are assumed to be at street level unless we have configuration to the contrary
     */
    private int streetToStopTime = 0;

    /**
     * @param stop The transit model stop reference.
     *             See {@link RoutingService#getStopVertexForStop()} for navigation
     *             from a Stop to this class.
     * @param modes Set of modes for all Routes using this stop. If {@code null} an empty set is used.
     */
    public TransitStopVertex (Graph graph, Stop stop, Set<TransitMode> modes) {
        super(graph, GtfsLibrary.convertIdToString(stop.getId()), stop.getLon(), stop.getLat());
        this.stop = stop;
        this.modes = modes != null ? modes : new HashSet<>();
        this.wheelchairEntrance = stop.getWheelchairBoarding() != WheelChairBoarding.NOT_POSSIBLE;
        //Adds this vertex into graph envelope so that we don't need to loop over all vertices
        graph.expandToInclude(stop.getLon(), stop.getLat());
    }

    public boolean hasWheelchairEntrance() {
        return wheelchairEntrance;
    }

    public boolean hasPathways() {
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

    public Set<TransitMode> getModes() {
        return modes;
    }

    public void addMode(TransitMode mode) {
        modes.add(mode);
    }

    public Stop getStop() {
            return this.stop;
    }

    @Override
    public StationElement getStationElement() {
        return this.stop;
    }
}
