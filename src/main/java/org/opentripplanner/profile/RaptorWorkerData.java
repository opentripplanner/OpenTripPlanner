package org.opentripplanner.profile;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Iterables;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RaptorWorkerData implements Serializable {

    public final int nStops;

    public final int nPatterns;

    /** For every stop, one pair of ints (targetStopIndex, distanceMeters) for each transfer out of that stop. */
    public final List<int[]> transfersForStop = new ArrayList<>();

    /** A list of pattern indexes passing through each stop. */
    public final List<int[]> patternsForStop = new ArrayList<>();

    /** An ordered list of stops visited by each pattern. */
    public final List<int[]> stopsForPattern = new ArrayList<>();

    /** For each pattern, a 2D array of stoptimes for each trip on the pattern. */
    public List<RaptorWorkerTimetable> timetablesForPattern = new ArrayList<>();

    /**
     * For each stop, one pair of ints (targetID, distanceMeters) for each destination near that stop.
     * For generic TimeSurfaces these are street intersections. They could be anything though since the worker doesn't
     * care what the IDs stand for. For example, they could be point indexes in a pointset.
     */
    public final List<int[]> targetsForStop = new ArrayList<>();;

    /** Optional debug data: the name of each stop. */
    public transient final TObjectIntMap<Stop> indexForStop;
    public transient final List<String> stopNames = new ArrayList<>();
    public transient final List<String> patternNames = new ArrayList<>();

    public RaptorWorkerData (Graph graph, TimeWindow window) {
        int totalPatterns = graph.index.patternForId.size();
        int totalStops = graph.index.stopForId.size();
        timetablesForPattern = new ArrayList<RaptorWorkerTimetable>(totalPatterns);
        List<TripPattern> patternForIndex = Lists.newArrayList(totalPatterns);
        TObjectIntMap<TripPattern> indexForPattern = new TObjectIntHashMap<>(totalPatterns, 0.75f, -1);
        indexForStop = new TObjectIntHashMap<>(totalStops, 0.75f, -1);
        List<Stop> stopForIndex = new ArrayList<>(totalStops);

        /* Make timetables for active trip patterns and record the stops each active pattern uses. */
        for (TripPattern pattern : graph.index.patternForId.values()) {
            RaptorWorkerTimetable timetable = RaptorWorkerTimetable.forPattern(graph, pattern, window);
            if (timetable == null) {
                // Pattern is not running during the time window
                continue;
            }
            timetablesForPattern.add(timetable);
            // Temporary bidirectional mapping between 0-based indexes and patterns
            indexForPattern.put(pattern, patternForIndex.size());
            patternForIndex.add(pattern);
            patternNames.add(pattern.code);
            TIntList stopIndexesForPattern = new TIntArrayList();
            for (Stop stop : pattern.getStops()) {
                int stopIndex = indexForStop.get(stop);
                if (stopIndex == -1) {
                    stopIndex = indexForStop.size();
                    indexForStop.put(stop, stopIndex);
                    stopForIndex.add(stop);
                    stopNames.add(stop.getName());
                }
                stopIndexesForPattern.add(stopIndex);
            }
            stopsForPattern.add(stopIndexesForPattern.toArray());
        }
        /** Fill in used pattern indexes for each used stop. */
        for (Stop stop : stopForIndex) {
            TIntList patterns = new TIntArrayList();
            for (TripPattern pattern : graph.index.patternsForStop.get(stop)) {
                int patternIndex = indexForPattern.get(pattern);
                if (patternIndex != -1) {
                    patterns.add(patternIndex);
                }
            }
            patternsForStop.add(patterns.toArray());
        }
        /** Record transfers between all used stops. */
        for (Stop stop : stopForIndex) {
            TIntList transfers = new TIntArrayList();
            TransitStop tstop = graph.index.stopVertexForStop.get(stop);
            for (SimpleTransfer simpleTransfer : Iterables.filter(tstop.getOutgoing(), SimpleTransfer.class)) {
                int targetStopIndex = indexForStop.get(((TransitStop) simpleTransfer.getToVertex()).getStop());
                if (targetStopIndex != -1) {
                    transfers.add(targetStopIndex);
                    transfers.add((int)(simpleTransfer.getDistance()));
                }
            }
            transfersForStop.add(transfers.toArray());
        }
        // Record distances to nearby intersections for all used stops.
        // This is just a copy of StopTreeCache using int indexes for stops.
        StopTreeCache stc = graph.index.getStopTreeCache();
        for (Stop stop : stopForIndex) {
            TransitStop tstop = graph.index.stopVertexForStop.get(stop);
            targetsForStop.add(stc.distancesForStop.get(tstop));
        }
        nStops = stopForIndex.size();
        nPatterns = patternForIndex.size();
    }

}
