package org.opentripplanner.profile;

import gnu.trove.map.TIntIntMap;

import org.opentripplanner.analyst.ResultSet;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.TimeSurface;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;

import java.util.Arrays;

/**
 * Stores times propagated to the street network in one-to-many repeated raptor profile routing.
 * At each raptor call, we find minimum travel times to each transit stop. Those must be propagated out to the street
 * network, giving minimum travel times to each street vertex. Those results are then merged into the summary
 * statistics per street vertex over the whole time window (over many RAPTOR calls).
 * This class handles the storage and merging of those summary statistics.
 */
public class PropagatedTimesStore {

    // Four parallel arrays has worse locality than one big 4|V|-length flat array, but merging per-raptor-call values
    // into this summary statistics storage is not the slow part of the algorithm. Optimization should concentrate on
    // the propagation of minimum travel times from transit stops to the street vertices.

    Graph graph;
    int size;
    int[] mins, maxs, sums, counts;

    public PropagatedTimesStore(Graph graph) {
        this(graph, Vertex.getMaxIndex());
    }

    public PropagatedTimesStore(Graph graph, int size) {
        this.graph = graph;

        this.size = size;
        mins = new int[size];
        maxs = new int[size];
        sums = new int[size];
        counts = new int[size];
        Arrays.fill(mins, Integer.MAX_VALUE);
    }

    public void setFromArray(int[][] times) {
        for (int i = 0; i < times.length; i++) {
            for (int v = 0; v < times[i].length; v++) {
                int newValue = times[i][v];

                if (newValue == RaptorWorker.UNREACHED)
                    continue;

                if (mins[v] > newValue) {
                    mins[v] = newValue;
                }
                if (maxs[v] < newValue) {
                    maxs[v] = newValue;
                }
                sums[v] += newValue;
                counts[v] += 1;
            }
        }
    }

    /**
     * Merge in min travel times to all vertices from one raptor call, finding minimum-of-min, maximum-of-min, and
     * average-of-min travel times.
     */
    public void mergeIn(int[] news) {
        for (int i = 0; i < size; i++) {
            int newValue = news[i];
            if (newValue == 0) {
                continue;
            }
            if (mins[i] > newValue) {
                mins[i] = newValue;
            }
            if (maxs[i] < newValue) {
                maxs[i] = newValue;
            }
            sums[i] += newValue;
            counts[i] += 1;
        }
    }

    /**
     * Merge in min travel times to all vertices from one raptor call, finding minimum-of-min, maximum-of-min, and
     * average-of-min travel times.
     */
    public void mergeIn(TIntIntMap news) {
        for (int i = 0; i < size; i++) {
            int newValue = news.get(i);
            if (newValue == 0) { // Trove default value is 0 like an array
                continue;
            }
            if (mins[i] > newValue) {
                mins[i] = newValue;
            }
            if (maxs[i] < newValue) {
                maxs[i] = newValue;
            }
            sums[i] += newValue;
            counts[i] += 1;
        }
    }

    /** You need to pass in a pre-constructed rangeSet because it requires a reference to the profile router. */
    public void makeSurfaces(TimeSurface.RangeSet rangeSet) {
        for (Vertex vertex : graph.index.vertexForId.values()) {
            int min = mins[vertex.getIndex()];
            int max = maxs[vertex.getIndex()];
            int sum = sums[vertex.getIndex()];
            int count = counts[vertex.getIndex()];
            if (count <= 0)
                continue;
            // Count is positive, extrema and sum must also be present
            rangeSet.min.times.put(vertex, min);
            rangeSet.max.times.put(vertex, max);
            rangeSet.avg.times.put(vertex, sum / count);
        }
    }

    /** Make a ResultSet directly given a sample set (must have constructed RaptorWorkerData from the same sampleset) */
    public ResultSet.RangeSet makeResults(SampleSet ss, boolean includeTimes) {
        ResultSet.RangeSet ret = new ResultSet.RangeSet();

        int[] avgs = new int[sums.length];

        for (int i = 0; i < ss.pset.capacity; i++) {
            int sum = sums[i];
            int count = counts[i];

            // Note: this is destructive
            if (count <= 0) {
                mins[i] = Integer.MAX_VALUE;
                maxs[i] = Integer.MAX_VALUE;
                avgs[i] = Integer.MAX_VALUE;
            }
            else {
                avgs[i] = sum / count;
            }
        }

        ret.min = new ResultSet(mins, ss.pset, includeTimes);
        ret.avg = new ResultSet(avgs, ss.pset, includeTimes);
        ret.max = new ResultSet(maxs, ss.pset, includeTimes);
        return ret;
    }
}