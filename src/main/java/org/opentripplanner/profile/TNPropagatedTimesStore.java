package org.opentripplanner.profile;

import com.vividsolutions.jts.geom.Coordinate;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.apache.commons.math3.util.FastMath;
import org.opentripplanner.analyst.PointSet;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.cluster.ResultEnvelope;
import org.opentripplanner.analyst.core.IsochroneData;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWD;
import org.opentripplanner.analyst.request.SampleGridRenderer.WTWDAccumulativeMetric;
import org.opentripplanner.common.geometry.AccumulativeGridSampler;
import org.opentripplanner.common.geometry.DelaunayIsolineBuilder;
import org.opentripplanner.common.geometry.SparseMatrixZSampleGrid;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.routing.graph.Vertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import static org.apache.commons.math3.util.FastMath.toRadians;

/**
 * This is an exact copy of PropagatedTimesStore that's being modified to work with (new) TransitNetworks
 * instead of (old) Graphs. We can afford the maintainability nightmare of duplicating so much code because this is
 * intended to completely replace the old class soon.
 * Its real purpose is to make something like a PointSetTimeRange out of many RAPTOR runs using the same PointSet as destinations.
 *
 * Stores travel times propagated to the search targets (a set of points of interest in a PointSet)
 * in one-to-many repeated raptor profile routing.
 *
 * Each RAPTOR call finds minimum travel times to each transit stop. Those must be propagated out to the final targets,
 * giving minimum travel times to each target (destination opportunity) in accessibility analysis.
 * Those results for one call are then merged into the summary statistics per target over the whole time window
 * (over many RAPTOR calls). This class handles the storage and merging of those summary statistics.
 *
 * Currently this includes minimum, maximum, and average earliest-arrival travel time for each target.
 * We could conceivably retain all the propagated times for every departure minute instead of collapsing them down into
 * three numbers. If they were all sorted, we could read off any quantile, including the median travel time.
 * Leaving them in departure time order would also be interesting, since you could then see visually how the travel time
 * varies as a function of departure time.
 *
 * We could also conceivably store travel time histograms per destination, but this entails a loss of information due
 * to binning into minutes. These binned times could not be used to apply a smooth sigmoid cutoff which we usually
 * do at one-second resolution.
 *
 * When exploring single-point (one-to-many) query results it would be great to have all these stored or produced on
 * demand for visualization.
 */
public class TNPropagatedTimesStore {

    private static final Logger LOG = LoggerFactory.getLogger(TNPropagatedTimesStore.class);

    // Four parallel arrays has worse locality than one big 4|V|-length flat array, but merging per-raptor-call values
    // into this summary statistics storage is not the slow part of the algorithm. Optimization should concentrate on
    // the propagation of minimum travel times from transit stops to the street vertices.
    int size;
    int[] mins, maxs, avgs;

    // number of times to bootstrap the mean.
    public final int N_BOOTSTRAPS = 400;

    private static final Random random = new Random();

    public TNPropagatedTimesStore(int size) {
        this.size = size;
        mins = new int[size];
        maxs = new int[size];
        avgs = new int[size];
        Arrays.fill(avgs, Integer.MAX_VALUE);
        Arrays.fill(mins, Integer.MAX_VALUE);
        Arrays.fill(maxs, Integer.MAX_VALUE);
    }

    /**
     * @param times for search (varying departure time), an array of travel times to each transit stop.
     */
    public void setFromArray(int[][] times, ConfidenceCalculationMethod confidenceCalculationMethod) {
        if (times.length == 0)
            // nothing to do
            return;

        // assume array is rectangular
        int stops = times[0].length;

        // cache random numbers. This should be fine as we're mixing it with the number of minutes
        // at which each destination is accessible, which is sometimes not 120, as well as the stop
        // position in the list (note that we have cleverly chosen a number which is a prime
        // so is not divisible by the number of iterations on the bootstrap). Finally recall that
        // the maximum number of times we're sampling from is generally 120 and we modulo this,
        // so the pigeonhole principle applies.
        // this is effectively a "random number generator" with phase 10007
        int[] randomNumbers = random.ints().limit(10007).map(Math::abs).toArray();
        int nextRandom = 0;

        // loop over stops on the outside so we can bootstrap
        STOPS: for (int stop = 0; stop < stops; stop++) {
            // compute the average
            int sum = 0;
            int count = 0;

            TIntList timeList = new TIntArrayList();

            ITERATIONS: for (int i = 0; i < times.length; i++) {
                if (times[i][stop] == RaptorWorker.UNREACHED)
                    continue ITERATIONS;

                sum += times[i][stop];
                count++;
                timeList.add(times[i][stop]);
            }

            if (count == 0)
                continue STOPS;

            avgs[stop] = sum / count;

            switch (confidenceCalculationMethod) {
            case BOOTSTRAP:
                // now bootstrap out a 95% confidence interval on the time
                int[] bootMeans = new int[N_BOOTSTRAPS];
                for (int boot = 0; boot < N_BOOTSTRAPS; boot++) {
                    int bsum = 0;

                    // sample from the Monte Carlo distribution with replacement
                    for (int iter = 0; iter < count; iter++) {
                        bsum += timeList
                                .get(randomNumbers[nextRandom++ % randomNumbers.length] % count);
                        //bsum += timeList.get(random.nextInt(count));
                    }

                    bootMeans[boot] = bsum / count;
                }

                Arrays.sort(bootMeans);
                // 2.5 percentile of distribution of means
                mins[stop] = bootMeans[N_BOOTSTRAPS / 40];
                // 97.5 percentile of distribution of means
                maxs[stop] = bootMeans[N_BOOTSTRAPS - N_BOOTSTRAPS / 40];
                break;
            case PERCENTILE:
                timeList.sort();
                mins[stop] = timeList.get(timeList.size() / 40);
                maxs[stop] = timeList.get(39 * timeList.size() / 40);
                break;
            case NONE:
                mins[stop] = maxs[stop] = avgs[stop];
                break;
            case MIN_MAX:
            default:
                mins[stop] = timeList.min();
                maxs[stop] = timeList.max();
                break;
            }
        }
    }

    /**
     * Make a ResultEnvelope directly from a given SampleSet.
     * The RaptorWorkerData must have been constructed from the same SampleSet.
     * This is how the accumulated results are returned back out to the PropagatedTimesStore's creator.
     */
    public ResultEnvelope makeResults(PointSet pointSet, boolean includeTimes, boolean includeHistograms, boolean includeIsochrones) {
        ResultEnvelope envelope = new ResultEnvelope();
        envelope.worstCase = new ResultSet(maxs, pointSet, includeTimes, includeHistograms, includeIsochrones);
        envelope.avgCase   = new ResultSet(avgs, pointSet, includeTimes, includeHistograms, includeIsochrones);
        envelope.bestCase  = new ResultSet(mins, pointSet, includeTimes, includeHistograms, includeIsochrones);
        return envelope;
    }

    /**
     * This bypasses a bunch of conversion and copy steps and just makes the isochrones.
     * This assumes that the target indexes in this router/propagatedTimesStore are vertex indexes, not pointset indexes.
     * ^^^^^^^^probably doesn't work currently, can we make an implicit pointset that just wraps the vertices?
     */
    public ResultEnvelope makeIsochronesForVertices () {
        ResultEnvelope envelope = new ResultEnvelope();
        envelope.worstCase = makeIsochroneForVertices(maxs);
        envelope.avgCase = makeIsochroneForVertices(avgs);
        envelope.bestCase = makeIsochroneForVertices(mins);
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
        final int offroadDistanceMeters = 250;

        SparseMatrixZSampleGrid<WTWD> grid = makeSampleGridForVertices(times, offroadDistanceMeters);
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


    // FIXME the following function requires a reference to the LinkedPointSet or the TransportNetwork. It should be elsewhere (PointSetTimeRange?)
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
        Coordinate coordinateOrigin = null; // FIXME new Coordinate(transitLayer.centerLon, transitLayer.centerLat);
        final double cosLat = FastMath.cos(toRadians(coordinateOrigin.y));
        double dY = Math.toDegrees(gridSizeMeters / SphericalDistanceLibrary.RADIUS_OF_EARTH_IN_M);
        double dX = dY / cosLat;
        grid = new SparseMatrixZSampleGrid<WTWD>(16, times.length, dX, dY, coordinateOrigin);
        AccumulativeGridSampler.AccumulativeMetric<WTWD> metric =
                new WTWDAccumulativeMetric(cosLat, offroadWalkDistance, offroadWalkSpeed, gridSizeMeters);
        AccumulativeGridSampler<WTWD> sampler =
                new AccumulativeGridSampler<>(grid, metric);
        // Iterate over every vertex, adding it to the ZSampleGrid if it was reached.
        Vertex vertex = null; // FIXME streetLayer.vertexStore.getCursor();
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
//            vertex.seek(v); FIXME need street layer to get vertex cursor
//            // FIXME we should propagate along street geometries here
//            if (vertex != null) {
//                sampler.addSamplingPoint(vertex.getCoordinate(), z, offroadWalkSpeed);
//            }
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

        /** Take the min and the max of the experienced of the experienced times. Only valid for scheduled services. */
        MIN_MAX
    }
}