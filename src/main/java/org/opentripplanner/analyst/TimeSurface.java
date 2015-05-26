package org.opentripplanner.analyst;

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.opentripplanner.common.geometry.AccumulativeGridSampler;
import org.opentripplanner.common.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.profile.AnalystProfileRouterPrototype;
import org.opentripplanner.profile.ProfileRequest;
import org.opentripplanner.profile.ProfileRouter;
import org.opentripplanner.profile.RepeatedRaptorProfileRouter;
import org.opentripplanner.profile.RoundBasedProfileRouter;
import org.opentripplanner.profile.TimeRange;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Map;

import static org.apache.commons.math3.util.FastMath.toRadians;

/**
 * A travel time surface. Timing information from the leaves of a ShortestPathTree.
 */
public class TimeSurface implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(TimeSurface.class);
    public static final int UNREACHABLE = -1;
    private static int nextId = 0;

    public final String routerId;
    public final int id;
    public final TObjectIntMap<Vertex> times = new TObjectIntHashMap<Vertex>(500000, 0.5f, UNREACHABLE);
    public final double lat, lon;
    public int cutoffMinutes = 90; // this should really be copied from the data source but the new repeated raptor does not do so
    public long dateTime;
    public Map<String, String> params; // The query params sent by the user, for reference only
    public SparseMatrixZSampleGrid<WTWD> sampleGrid; // another representation on a regular grid with a triangulation
    public String description;
    public double walkSpeed = 1.33; // meters/sec TODO could we just store the whole routing request instead of params?

    /** Create a time surface with a sample grid */
    public TimeSurface(ShortestPathTree spt) {
        this(spt, true);
    }
    
    /** Create a time surface, optionally making a sample grid */
    public TimeSurface(ShortestPathTree spt, boolean makeSampleGrid) {

        params = spt.getOptions().parameters;
        walkSpeed = spt.getOptions().walkSpeed;

        String routerId = spt.getOptions().routerId;
        if (routerId == null || routerId.isEmpty() || routerId.equalsIgnoreCase("default")) {
            routerId = "default";
        }
        // Here we use the key "default" unlike the graphservice which substitutes in the default ID.
        // We don't want to keep that default in sync across two modules.
        this.routerId = routerId;
        long t0 = System.currentTimeMillis();
        for (State state : spt.getAllStates()) {
            Vertex vertex = state.getVertex();
            if (vertex instanceof StreetVertex || vertex instanceof TransitStop) {
                int existing = times.get(vertex);
                int t = (int) state.getActiveTime();
                if (existing == UNREACHABLE || existing > t) {
                    times.put(vertex, t);
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
        
        if (makeSampleGrid)
            makeSampleGrid(spt);
    }

    /** Make a max or min timesurface from propagated times in a ProfileRouter. */
    public TimeSurface (AnalystProfileRouterPrototype profileRouter) {
        ProfileRequest req = profileRouter.request;
        lon = req.fromLon;
        lat = req.fromLat;
        id = makeUniqueId();
        dateTime = req.fromTime; // FIXME
        routerId = profileRouter.graph.routerId;
        cutoffMinutes = profileRouter.MAX_DURATION / 60;
        walkSpeed = profileRouter.request.walkSpeed;
    }

    /** Make a max or min timesurface from propagated times in a ProfileRouter. */
    public TimeSurface (ProfileRouter profileRouter) {
        // TODO merge with the version that takes AnalystProfileRouterPrototype, they are exactly the same.
        // But those two classes are not in the same inheritance hierarchy.
        ProfileRequest req = profileRouter.request;
        lon = req.fromLon;
        lat = req.fromLat;
        id = makeUniqueId();
        dateTime = req.fromTime; // FIXME
        routerId = profileRouter.graph.routerId;
        cutoffMinutes = profileRouter.MAX_DURATION / 60;
        walkSpeed = profileRouter.request.walkSpeed;
    }
    
    /** Make a max or min timesurface from propagated times in a ProfileRouter. */
    public TimeSurface (RoundBasedProfileRouter profileRouter) {
        ProfileRequest req = profileRouter.request;
        lon = req.fromLon;
        lat = req.fromLat;
        id = makeUniqueId();
        dateTime = req.fromTime; // FIXME
        routerId = profileRouter.graph.routerId;
    }

    public TimeSurface(RepeatedRaptorProfileRouter profileRouter) {
        ProfileRequest req = profileRouter.request;
        lon = req.fromLon;
        lat = req.fromLat;
        id = makeUniqueId();
        dateTime = req.fromTime; // FIXME
        routerId = profileRouter.graph.routerId;
        cutoffMinutes = 120; // FIXME is there any well-defined cutoff? This is needed for generating isochrone curves.
    }

	public static TimeSurface.RangeSet makeSurfaces (AnalystProfileRouterPrototype profileRouter) {
        TimeSurface minSurface = new TimeSurface(profileRouter);
        TimeSurface avgSurface = new TimeSurface(profileRouter);
        TimeSurface maxSurface = new TimeSurface(profileRouter);
        for (Map.Entry<Vertex, TimeRange> vtr : profileRouter.propagatedTimes.entrySet()) {
            Vertex v = vtr.getKey();
            TimeRange tr = vtr.getValue();
            minSurface.times.put(v, tr.min);
            avgSurface.times.put(v, tr.avg);
            maxSurface.times.put(v, tr.max);
        }
        RangeSet result = new RangeSet();
        minSurface.description = "Travel times assuming best luck (never waiting for a transfer).";
        avgSurface.description = "Expected travel times (average wait for every transfer).";
        maxSurface.description = "Travel times assuming worst luck (maximum wait for every transfer).";
        result.min = minSurface;
        result.avg = avgSurface;
        result.max = maxSurface;
        return result;
    }

    /** Groups together three TimeSurfaces as a single response for profile-analyst. */
    public static class RangeSet {
        public TimeSurface min;
        public TimeSurface avg;
        public TimeSurface max;
    }

    public int getTime(Vertex v) {
        return times.get(v);
    }

    private synchronized int makeUniqueId() {
        int id = nextId++;
        return id;
    }

    public int size() { return nextId; }

    // TODO Lazy-initialize sample grid on demand so initial SPT finishes faster, and only isolines lag behind.
    // however, the existing sampler needs an SPT, not general vertex-time mappings.
    private void makeSampleGrid (ShortestPathTree spt) {
        long t0 = System.currentTimeMillis();
        final double gridSizeMeters = 300; // Todo: set dynamically and make sure this matches isoline builder params
        final double V0 = 1.00; // off-road walk speed in m/sec
        Coordinate coordinateOrigin = new Coordinate();
        final double cosLat = FastMath.cos(toRadians(coordinateOrigin.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;
        sampleGrid = new SparseMatrixZSampleGrid<WTWD>(16, spt.getVertexCount(), dX, dY, coordinateOrigin);
        SampleGridRenderer.sampleSPT(spt, sampleGrid, gridSizeMeters * 0.7, gridSizeMeters, V0, spt
                .getOptions().getMaxWalkDistance(), Integer.MAX_VALUE, cosLat);
        long t1 = System.currentTimeMillis();
        LOG.info("Made SampleGrid from SPT in {} msec.", (int) (t1 - t0));
    }

    /**
     * Create the SampleGrid from whatever values are already in the TimeSurface, rather than looking at the SPT.
     * This is not really ideal since it includes only intersection nodes, and no points along the road segments.
     */
    public void makeSampleGridWithoutSPT () {
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
        sampleGrid = new SparseMatrixZSampleGrid<WTWD>(16, this.times.size(), dX, dY, coordinateOrigin);
        AccumulativeGridSampler.AccumulativeMetric<WTWD> metric = new SampleGridRenderer.WTWDAccumulativeMetric(cosLat, D0, V0, gridSizeMeters);
        AccumulativeGridSampler<WTWD> sampler = new AccumulativeGridSampler<WTWD>(sampleGrid, metric);
        // Iterate over every vertex in this timesurface, adding it to the ZSampleGrid
        // TODO propagation along street geometries could happen at this stage, rather than when the SPT is still available.
        for (TObjectIntIterator<Vertex> iter = times.iterator(); iter.hasNext(); ) {
            iter.advance();
            Vertex vertex = iter.key();
            int time = iter.value();
            WTWD z = new WTWD();
            z.w = 1.0;
            z.d = 0.0;
            z.wTime = time;
            z.wBoardings = 0; // unused
            z.wWalkDist = 0; // unused
            sampler.addSamplingPoint(vertex.getCoordinate(), z, V0);
        }
        sampler.close();
        long t1 = System.currentTimeMillis();
        LOG.info("Made scalar SampleGrid from TimeSurface in {} msec.", (int) (t1 - t0));
    }

    /**
     * TODO A trivial TZ class containing only a single scalar, or better yet a scalar grid class using primitives.
     * When a new instance is created, it should be "empty" until values are accumulated into it: all its fields should
     * be zero except the minimum off-road distance, which should be positive infinity.
     */

}
