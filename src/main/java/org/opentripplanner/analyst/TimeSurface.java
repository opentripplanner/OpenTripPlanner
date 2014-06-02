package org.opentripplanner.analyst;

import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.util.Arrays;
import java.util.Map;

/**
 * A travel time surface. Timing information from the leaves of a ShortestPathTree.
 * In Portland, one timesurface takes roughly one MB of memory and is also about that size as JSON.
 * However it is proportionate to the graph size not the time cutoff.
 */
public class TimeSurface {

    public static final int UNREACHABLE = -1;
    private static int nextId = 0;

    public final int id;
    public final int[] times; // one time in seconds per vertex
    public final double lat, lon;
    public Map<String, String> params; // The query params sent by the user, for reference only

    public TimeSurface(ShortestPathTree spt) {
        times = new int[Vertex.getMaxIndex()]; // memory leak due to temp vertices?
        Arrays.fill(times, UNREACHABLE);
        for (State state : spt.getAllStates()) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof StreetVertex || vertex instanceof TransitStop) {
                int i = vertex.getIndex();
                int t = (int) state.getActiveTime();
                if (times[i] == UNREACHABLE || times[i] > t) {
                    times[i] = t;
                }
            }
        }
        GenericLocation from = spt.getOptions().getFrom();
        this.lon = from.getLng();
        this.lat = from.getLat();
        this.id = makeUniqueId();
    }

    public int getTime(Vertex v) {
        return times[v.getIndex()];
    }

    private synchronized int makeUniqueId() {
        int id = nextId++;
        return id;
    }

}