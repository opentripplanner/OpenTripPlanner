package org.opentripplanner.profile;

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.analyst.cluster.ResultEnvelope;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.request.SampleGridRenderer;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWDAccumulativeMetric;
import org.opentripplanner.common.geometry.AccumulativeGridSampler;
import org.opentripplanner.common.geometry.DelaunayIsolineBuilder;
import org.opentripplanner.common.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.apache.commons.math3.util.FastMath.toRadians;

/**
 * Stores travel times propagated to the search targets (all vertices in the street network or a set of points of
 * interest) in one-to-many repeated raptor profile routing.
 *
 * Each raptor call finds minimum travel times to each transit stop. Those must be propagated out to the final targets,
 * giving minimum travel times to each street vertex (or each destination opportunity in accessibility analysis).
 * Those results are then merged into the summary statistics per target over the whole time window
 * (over many RAPTOR calls). This class handles the storage and merging of those summary statistics.
 *
 * Currently this includes minimum, maximum, and average earliest-arrival travel time for each target.
 * We could conceivably retain all the propagated times for every departure minute instead of collapsing them down into
 * three numbers. If they were all sorted, we could read off any quantile, including the median travel time.
 * Leaving them in departure time order would also be interesting, since you could then see visually how the travel time
 * varies as a function of departure time.
 * We could also conceivably store travel time histograms per destination, but this entails a loss of information due
 * to binning into minutes. These binned times could not be used to apply a smooth sigmoid cutoff which we usually
 * do at one-second resolution.
 *
 * When exploring single-point (one-to-many) query results it would be great to have all these stored or produced on
 * demand for visualization.
 */
public class PropagatedTimesStore {

    private static final Logger LOG = LoggerFactory.getLogger(PropagatedTimesStore.class);

    // Four parallel arrays has worse locality than one big 4|V|-length flat array, but merging per-raptor-call values
    // into this summary statistics storage is not the slow part of the algorithm. Optimization should concentrate on
    // the propagation of minimum travel times from transit stops to the street vertices.

    Graph graph;
    int size;
    int[] mins, maxs, avgs;
    ProfileRequest req;

    // number of times to bootstrap the mean.
    public final int N_BOOTSTRAPS = 400;

    private static final Random random = new Random();

    public PropagatedTimesStore(Graph graph, ProfileRequest req) {
        this(graph, req, Vertex.getMaxIndex());
    }

    public PropagatedTimesStore(Graph graph, ProfileRequest req, int size) {
        this.graph = graph;
        this.req = req;

        this.size = size;
        mins = new int[size];
        maxs = new int[size];
        avgs = new int[size];
        Arrays.fill(avgs, Integer.MAX_VALUE);
        Arrays.fill(mins, Integer.MAX_VALUE);
        Arrays.fill(maxs, Integer.MAX_VALUE);
    }

    /**
     * @param times for search (varying departure time), an array of travel times to each destination.
     * @param includeInAverages for each iteration, whether that iteration should be included in average calculations.
     *                          In RaptorWorker's Monte Carlo code we also include minima and maxima, which should
     *                          not be included in averages.
     *                          Iterations that are not included in averages are still used to determine extrema.
     */
    public void setFromArray(int[][] times, boolean[] includeInAverages, ConfidenceCalculationMethod confidenceCalculationMethod) {
        if (times.length == 0)
            // nothing to do
            return;

        // assume array is rectangular
        int nTargets = times[0].length;

        // cache random numbers. This should be fine as we're mixing it with the number of minutes
        // at which each destination is accessible, which is sometimes not 120, as well as the stop
        // position in the list (note that we have cleverly chosen a number which is a prime
        // so is not divisible by the number of iterations on the bootstrap). Finally recall that
        // the maximum number of times we're sampling from is generally 120 and we modulo this,
        // so the pigeonhole principle applies.
        // this is effectively a "random number generator" with phase 10007
        int[] randomNumbers = random.ints().limit(10007).map(Math::abs).toArray();
        int nextRandom = 0;

        int effectiveIterations = 0;
        for (int i = 0; i < includeInAverages.length; i++) {
            if (includeInAverages[i]) effectiveIterations++;
        }

        // loop over targets on the outside so we can bootstrap
        TARGETS: for (int target = 0; target < nTargets; target++) {
            // compute the average
            int sum = 0;
            int count = 0;

            TIntList timeList = new TIntArrayList();
            TIntList avgList = new TIntArrayList();

            ITERATIONS: for (int i = 0; i < times.length; i++) {
                if (times[i][target] == RaptorWorker.UNREACHED)
                    continue ITERATIONS;

                if (includeInAverages[i]) {
                    avgList.add(times[i][target]);
                    sum += times[i][target];
                    count++;
                }

                timeList.add(times[i][target]);
            }

            // never reachable
            if (count == 0)
                continue TARGETS;

            // if the destination is reachable less than half the time, consider it unreachable "on average".
            // This avoids issues where destinations are reachable for some very small percentage of the time, either because
            // there is a single departure near the start of the time window, or because they take approximately 2 hours
            // (the default maximum cutoff) to reach.

            // Consider a search run with time window 7AM to 9AM, and an origin and destination connected by an express
            // bus that runs once at 7:05. For the first five minutes of the time window, accessibility is very good.
            // For the rest, there is no accessibility; if we didn't have this rule in place, the average would be the average
            // of the time the destination is reachable, and the time it is unreachable would be excluded from the calculation
            // (see issue 2148)

            // There is another issue that this rule does not completely address. Consider a trip that takes 1:45
            // exclusive of wait time and runs every half-hour. Half the time it takes less than two hours and is considered
            // and half the time it takes more than two hours and is excluded, so the average is biased low on very long trips.
            // This rule catches the most egregious cases (say where we average only the best four minutes out of a two-hour
            // span) but does not completely address the issue. However if you're looking at a time cutoff significantly
            // less than two hours, it's not a big problem. Significantly less is half the headway of your least-frequent service, because
            // if there is a trip on your least-frequent service that takes on average the time cutoff plus one minute
            // it will be unbiased and considered unreachable iff the longest trip is less than two hours, which it has
            // to be if the time cutoff plus half the headway is less than two hours, assuming a symmetric travel time
            // distribution.

            // TODO: due to multiple paths to a target the distribution is not symmetrical though - evaluate the
            // effect of this. Also, transfers muddy the concept of "worst frequency" since there is variation in mid-trip
            // wait times as well.
            if (count >= effectiveIterations * req.reachabilityThreshold)
                avgs[target] = sum / count;

            // TODO: correctly handle partial accessibility for bootstrap and percentile options.
            switch (confidenceCalculationMethod) {
            case BOOTSTRAP:
                // now bootstrap out a 95% confidence interval on the time
                int[] bootMeans = new int[N_BOOTSTRAPS];

                nextRandom += N_BOOTSTRAPS * count % randomNumbers.length; // prevent overflow

                final int randOff = nextRandom;
                final int finalCount = count;

                IntStream.range(0, N_BOOTSTRAPS).parallel().forEach(boot -> {
                    int bsum = 0;

                    // sample from the Monte Carlo distribution with replacement
                    for (int iter = 0; iter < finalCount; iter++) {
                        bsum += avgList
                                .get(randomNumbers[(randOff + boot * iter) % randomNumbers.length] % avgList.size());
                        //bsum += timeList.get(random.nextInt(count));
                    }

                     bootMeans[boot] = bsum / finalCount;
                });

                Arrays.sort(bootMeans);
                // 2.5 percentile of distribution of means
                mins[target] = bootMeans[N_BOOTSTRAPS / 40];
                // 97.5 percentile of distribution of means
                maxs[target] = bootMeans[N_BOOTSTRAPS - N_BOOTSTRAPS / 40];
                break;
            case PERCENTILE:
                timeList.sort();
                mins[target] = timeList.get(timeList.size() / 40);
                maxs[target] = timeList.get(39 * timeList.size() / 40);
                break;
            case NONE:
                mins[target] = maxs[target] = avgs[target];
                break;
            case MIN_MAX:
            default:
                mins[target] = timeList.min();

                // worst case: if it is sometimes unreachable, worst case is unreachable; otherwise use the max from the
                // time list.
                // NB not using count here as it doesn't count iterations that are not included in averages
                if (timeList.size() == times.length)
                    maxs[target] = timeList.max();

                break;
            }
        }
    }

    /**
     * Make a ResultEnvelope directly from a given SampleSet.
     * The RaptorWorkerData must have been constructed from the same SampleSet.
     */
    public ResultEnvelope makeResults(SampleSet ss, boolean includeTimes, boolean includeHistograms, boolean includeIsochrones) {
        ResultEnvelope envelope = new ResultEnvelope();
        // max times == worst case accessibility
        envelope.worstCase = new ResultSet(maxs, ss.pset, includeTimes, includeHistograms, includeIsochrones);
        envelope.avgCase   = new ResultSet(avgs, ss.pset, includeTimes, includeHistograms, includeIsochrones);
        envelope.bestCase  = new ResultSet(mins, ss.pset, includeTimes, includeHistograms, includeIsochrones);
        return envelope;
    }

    public TimeSurface.RangeSet makeSurfaces(RepeatedRaptorProfileRouter repeatedRaptorProfileRouter) {
        TimeSurface.RangeSet rangeSet = new TimeSurface.RangeSet();
        rangeSet.min = new TimeSurface(repeatedRaptorProfileRouter);
        rangeSet.avg = new TimeSurface(repeatedRaptorProfileRouter);
        rangeSet.max = new TimeSurface(repeatedRaptorProfileRouter);
        for (Vertex vertex : graph.index.vertexForId.values()) {
            int min = mins[vertex.getIndex()];
            int max = maxs[vertex.getIndex()];
            int avg = avgs[vertex.getIndex()];
            if (avg == Integer.MAX_VALUE)
                continue;
            // Count is positive, extrema and sum must also be present
            rangeSet.min.times.put(vertex, min);
            rangeSet.max.times.put(vertex, max);
            rangeSet.avg.times.put(vertex, avg);
        }
        return rangeSet;
    }

    /**
     * This bypasses a bunch of conversion and copy steps and just makes the isochrones.
     * This assumes that the target indexes in this router/propagatedTimesStore are vertex indexes, not pointset indexes.
     * TODO parameter for a pointset or a vertex lookup table, so we can handle both.
     */
    public ResultEnvelope makeIsochronesForVertices () {
        ResultEnvelope envelope = new ResultEnvelope();
        envelope.bestCase = makeIsochroneForVertices(mins);
        envelope.avgCase = makeIsochroneForVertices(avgs);
        envelope.worstCase = makeIsochroneForVertices(maxs);
        return envelope;
    }

    /**
     * This bypasses a bunch of TimeSurface conversion/copy steps we were going though and makes the isochrones directly.
     * This assumes that the target indexes in this router/propagatedTimesStore are vertex indexes, not pointset indexes.
     * Called three times on min/avg/max to create the three elements of a ResultEnvelope.
     */
    private ResultSet makeIsochroneForVertices (int[] times) {

        final int spacing = 5;
        final int nMax = 24;
        final int cutoffMinutes = 120;
        final double gridSize = IsochroneGenerator.GRID_SIZE_METERS;
        final double offroadDistanceMeters = gridSize * IsochroneGenerator.WALK_DISTANCE_GRID_SIZE_RATIO;

        SparseMatrixZSampleGrid<WTWD> grid = makeSampleGridForVertices(times, gridSize);
        long t0 = System.currentTimeMillis();
        DelaunayIsolineBuilder<WTWD> isolineBuilder =
                new DelaunayIsolineBuilder<>(grid.delaunayTriangulate(), new WTWD.IsolineMetric());

        List<IsochroneData> isoData = new ArrayList<IsochroneData>();
        for (int minutes = spacing, n = 0; minutes <= cutoffMinutes && n < nMax; minutes += spacing, n++) {
            int seconds = minutes * 60;
            WTWD z0 = new WTWD();
            z0.w = 1.0;
            z0.wTime = seconds;
            z0.d = offroadDistanceMeters;
            IsochroneData isochrone = new IsochroneData(seconds, isolineBuilder.computeIsoline(z0));
            isoData.add(isochrone);
        }

        long t1 = System.currentTimeMillis();
        ResultSet resultSet = new ResultSet();
        resultSet.isochrones = new IsochroneData[isoData.size()];
        isoData.toArray(resultSet.isochrones);
        LOG.debug("Computed {} isochrones in {} msec", isoData.size(), (int) (t1 - t0));
        return resultSet;
    }

    /**
     * Create a SampleGrid from only the times stored in this PropagatedTimesStore.
     * This assumes that the target indexes in this router/propagatedTimesStore are vertex indexes, not pointset indexes.
     * This is not really ideal since it includes only intersection nodes, and no points along the road segments.
     * FIXME this may be why we're getting hole-punching failures.
     * TODO: rewrite the isoline code to use only primitive collections and operate on a scalar field.
     */
    public SparseMatrixZSampleGrid<WTWD> makeSampleGridForVertices (int[] times, final double gridSizeMeters) {
        SparseMatrixZSampleGrid<WTWD> grid;
        long t0 = System.currentTimeMillis();
        // Off-road max distance MUST be APPROX EQUALS to the grid precision
        // TODO: Loosen this restriction (by adding more closing sample).
        // Change the 0.8 magic factor here with caution. Should be roughly grid size.
        final double offroadWalkDistance = 0.8 * gridSizeMeters;
        final double offroadWalkSpeed = 1.00; // in m/sec
        Coordinate coordinateOrigin = graph.getCenter().get();
        final double cosLat = FastMath.cos(toRadians(coordinateOrigin.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;
        grid = new SparseMatrixZSampleGrid<WTWD>(16, times.length, dX, dY, coordinateOrigin);
        AccumulativeGridSampler.AccumulativeMetric<SampleGridRenderer.WTWD> metric =
                new WTWDAccumulativeMetric(cosLat, offroadWalkDistance, offroadWalkSpeed, gridSizeMeters);
        AccumulativeGridSampler<WTWD> sampler =
                new AccumulativeGridSampler<>(grid, metric);
        // Iterate over every vertex, adding it to the ZSampleGrid if it was reached.
        for (int v = 0; v < times.length; v++) {
            int time = times[v];
            if (time == Integer.MAX_VALUE) {
                continue; // MAX_VALUE is the "unreached" value
            }
            WTWD z = new WTWD();
            z.w = 1.0;
            z.d = 0.0;
            z.wTime = time;
            z.wBoardings = 0; // unused
            z.wWalkDist = 0; // unused
            Vertex vertex = graph.getVertexById(v); // FIXME ack, this uses a hashtable and autoboxing!
            // FIXME we should propagate along street geometries here
            if (vertex != null) {
                sampler.addSamplingPoint(vertex.getCoordinate(), z, offroadWalkSpeed);
            }
        }
        sampler.close();
        long t1 = System.currentTimeMillis();
        LOG.info("Made scalar SampleGrid from TimeSurface in {} msec.", (int) (t1 - t0));
        return grid;
    }

    public int countTargetsReached() {
        int count = 0;
        for (int min : mins) {
            if (min != RaptorWorker.UNREACHED) {
                count++;
            }
        }
        return count;
    }

    public static enum ConfidenceCalculationMethod {
        /** Do not calculate confidence intervals */
        NONE,

        /**
         * Calculate confidence intervals around the mean using the bootstrap. Note that this calculates
         * the confidence that the mean is in fact the mean of all possible schedules, not the confidence
         * that a particular but unknown schedule will behave a certain way.
         *
         * This is absolutely the correct approach in systems that are specified as frequencies both
         * in the model and operationally, because the parameter of interest is the average accessibility
         * afforded by every realization of transfer and wait time. This yields nice tiny confidence
         * intervals around the mean, and allows us easily to measure changes in average accessibility.
         *
         * However, when you have a system that will eventually be scheduled, you are interested not
         * in the distribution of the average accessibility over all possible schedules, but rather
         * the distribution of the accessibility afforded by a particular but unknown schedule. This
         * does not require bootstrapping; it's just taking percentiles on the output of the Monte
         * Carlo simulation. Unfortunately this requires a lot more Monte Carlo samples, as of
         * course the middle of the distribution will stabilize long before the extrema.
         */
        BOOTSTRAP,

        /**
         * Calculate confidence intervals based on percentiles, which is what you want to do when
         * you have a scheduled network.
         */
        PERCENTILE,

        /**
         * Take the min and the max of the experienced of the experienced times. Monte Carlo simulations also include
         * one run with worst-case and one run with best-case boarding, so this is valid even for frequency service.
         */
        MIN_MAX
    }
}