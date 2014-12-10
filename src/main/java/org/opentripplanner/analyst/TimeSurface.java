package org.opentripplanner.analyst;

import com.google.common.collect.Lists;
import com.vividsolutions.jts.geom.Coordinate;

import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.common.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.geometry.ZSampleGrid;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.ProfileRouter;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.apache.commons.math3.util.FastMath.toRadians;

/**
 * A travel time surface. Timing information from the leaves of a ShortestPathTree.
 * In Portland, one timesurface takes roughly one MB of memory and is also about that size as JSON.
 * However it is proportionate to the graph size not the time cutoff.
 */
public class TimeSurface implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSurface.class);
    public static final int UNREACHABLE = -1;
    private static int nextId = 0;

    public final String routerId;
    
    public final int id;
    public final int[] times; // one time in seconds per vertex
    public final double lat, lon;
    public int cutoffMinutes;
    public long dateTime;
    public Map<String, String> params; // The query params sent by the user, for reference only

    public SparseMatrixZSampleGrid<WTWD> sampleGrid; // another representation on a regular grid with a triangulation

    public TimeSurface(ShortestPathTree spt) {
    	
    	params = spt.getOptions().parameters;
    	
        String routerId = spt.getOptions().routerId;
        if (routerId == null || routerId.isEmpty() || routerId.equalsIgnoreCase("default")) {
            routerId = "default";
        }
        // Here we use the key "default" unlike the graphservice which substitutes in the default ID.
        // We don't want to keep that default in sync across two modules.
    	this.routerId = routerId;
        long t0 = System.currentTimeMillis();
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
        
        // TODO make this work as either to or from query
        GenericLocation from = spt.getOptions().from;
        this.lon = from.lng;
        this.lat = from.lat;
        this.id = makeUniqueId();
        this.dateTime = spt.getOptions().dateTime;
        long t1 = System.currentTimeMillis();
        LOG.info("Made TimeSurface from SPT in {} msec.", (int) (t1 - t0));
        makeSampleGrid(spt);
    }

    /** Make a max or min timesurface from propagated times in a ProfileRouter. */
    public TimeSurface (ProfileRouter profileRouter, boolean maxNotMin) {
        ProfileRequest req = profileRouter.request;
        lon = req.from.lon;
        lat = req.from.lat;
        id = makeUniqueId();
        dateTime = req.fromTime; // FIXME
        routerId = profileRouter.graph.routerId;
        cutoffMinutes = profileRouter.MAX_DURATION / 60;
        times = maxNotMin ? profileRouter.maxs : profileRouter.mins;
    }

    public int getTime(Vertex v) {
        return times[v.getIndex()];
    }

    private synchronized int makeUniqueId() {
        int id = nextId++;
        return id;
    }

    public int size() { return nextId; }

    // TODO Lazy-initialize sample grid on demand so initial SPT finishes faster, and only isolines lag behind.
    // however, the existing sampler needs an SPT, not general vertex-time mappings.
    public void makeSampleGrid (ShortestPathTree spt) {
        long t0 = System.currentTimeMillis();
        final double gridSizeMeters = 300; // Todo: set dynamically and make sure this matches isoline builder params
        // Off-road max distance MUST be APPROX EQUALS to the grid precision
        // TODO: Loosen this restriction (by adding more closing sample).
        // Change the 0.8 magic factor here with caution.
        final double D0 = 0.8 * gridSizeMeters; // offroad walk distance roughly grid size
        final double V0 = 1.00; // off-road walk speed in m/sec
        Coordinate coordinateOrigin = new Coordinate();
        final double cosLat = FastMath.cos(toRadians(coordinateOrigin.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;
        sampleGrid = new SparseMatrixZSampleGrid<WTWD>(16, spt.getVertexCount(), dX, dY, coordinateOrigin);
        SampleGridRenderer.sampleSPT(spt, sampleGrid, gridSizeMeters * 0.7, gridSizeMeters, V0, spt.getOptions().getMaxWalkDistance(), Integer.MAX_VALUE, cosLat);
        long t1 = System.currentTimeMillis();
        LOG.info("Made SampleGrid from SPT in {} msec.", (int) (t1 - t0));
    }
}