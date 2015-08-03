package org.opentripplanner.profile;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.util.Arrays;
import java.util.Random;

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

    // Four parallel arrays has worse locality than one big 4|V|-length flat array, but merging per-raptor-call values
    // into this summary statistics storage is not the slow part of the algorithm. Optimization should concentrate on
    // the propagation of minimum travel times from transit stops to the street vertices.

    Graph graph;
    int size;
    int[] mins, maxs, avgs;

    private static final Random random = new Random();

    public PropagatedTimesStore(Graph graph) {
        this(graph, Vertex.getMaxIndex());
    }

    public PropagatedTimesStore(Graph graph, int size) {
        this.graph = graph;

        this.size = size;
        mins = new int[size];
        maxs = new int[size];
        avgs = new int[size];
        Arrays.fill(avgs, Integer.MAX_VALUE);
        Arrays.fill(mins, Integer.MAX_VALUE);
        Arrays.fill(maxs, Integer.MAX_VALUE);
    }

    public void setFromArray(int[][] times) {
        if (times.length == 0)
            // nothing to do
            return;

        // assume array is rectangular
        int stops = times[0].length;

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

            // now bootstrap out a 95% confidence interval on the time
            int[] bootMeans = new int[400];
            for (int boot = 0; boot < 400; boot++) {
                int bsum = 0;

                // sample from the Monte Carlo distribution with replacement
                for (int iter = 0; iter < count; iter++) {
                    bsum += timeList.get(random.nextInt(count));
                }

                bootMeans[boot] = bsum / count;
            }

            Arrays.sort(bootMeans);
            // 2.5 percentile of distribution of means
            mins[stop] = bootMeans[10];
            // 97.5 percentile of distribution of means
            maxs[stop] = bootMeans[390];
        }
    }

    /** You need to pass in a pre-constructed rangeSet because it requires a reference to the profile router. */
    public void makeSurfaces(TimeSurface.RangeSet rangeSet) {
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
    }

    /** Make a ResultSet directly given a sample set (must have constructed RaptorWorkerData from the same sampleset) */
    public ResultSet.RangeSet makeResults(SampleSet ss, boolean includeTimes, boolean includeHistograms, boolean includeIsochrones) {
        ResultSet.RangeSet ret = new ResultSet.RangeSet();

        ret.min = new ResultSet(mins, ss.pset, includeTimes, includeHistograms, includeIsochrones);
        ret.avg = new ResultSet(avgs, ss.pset, includeTimes, includeHistograms, includeIsochrones);
        ret.max = new ResultSet(maxs, ss.pset, includeTimes, includeHistograms, includeIsochrones);
        return ret;
    }
}