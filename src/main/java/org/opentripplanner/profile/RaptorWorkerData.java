package org.opentripplanner.profile;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.scenario.AddTripPattern;
import org.opentripplanner.analyst.scenario.Scenario;
import org.opentripplanner.analyst.scenario.TripPatternFilter;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class RaptorWorkerData implements Serializable {
    public static final Logger LOG = LoggerFactory.getLogger(RaptorWorkerData.class);

    public final int nStops;

    public final int nPatterns;

    /** The number of targets (vertices or samples) */
    public final int nTargets;

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
    public transient final TIntIntMap indexForStop;
    public transient final List<String> stopNames = new ArrayList<>();
    public transient final List<String> patternNames = new ArrayList<>();

    /** Create RaptorWorkerData for the given window and graph, with the specified routes (specified as agencyid_routeid) banned */
    public RaptorWorkerData (Graph graph, TimeWindow window, Scenario scenario) {
        this(graph, window, scenario, null);
    }

    /** Create RaptorWorkerData to be used to build ResultSets directly without creating an intermediate SampleSet */
    public RaptorWorkerData (Graph graph, TimeWindow window, Scenario scenario, SampleSet sampleSet) {
        int totalPatterns = graph.index.patternForId.size();
        int totalStops = graph.index.stopForId.size();
        timetablesForPattern = new ArrayList<RaptorWorkerTimetable>(totalPatterns);
        List<TripPattern> patternForIndex = Lists.newArrayList(totalPatterns);
        TObjectIntMap<TripPattern> indexForPattern = new TObjectIntHashMap<>(totalPatterns, 0.75f, -1);
        indexForStop = new TIntIntHashMap();
        TIntList stopForIndex = new TIntArrayList(totalStops);

        /* Make timetables for active trip patterns and record the stops each active pattern uses. */
        for (TripPattern originalPattern : graph.index.patternForId.values()) {
            Collection<TripPattern> patterns = Arrays.asList(originalPattern);

            // apply filters. note that a filter can create multiple trip patterns from a single trip pattern
            // so we need to make sure we handle all of them
            if (scenario != null && scenario.modifications != null) {
                for (TripPatternFilter filter : Iterables
                        .filter(scenario.modifications, TripPatternFilter.class)) {
                    Collection<TripPattern> modifiedPatterns = Lists.newArrayList();

                    for (TripPattern pattern : patterns) {
                        Collection<TripPattern> result = filter.apply(pattern);

                        if (result != null)
                            modifiedPatterns.addAll(result);
                    }

                    // this is the result of this filter for all trip patterns
                    patterns = modifiedPatterns;
                }
            }

            for (TripPattern pattern : patterns) {
                RaptorWorkerTimetable timetable = RaptorWorkerTimetable
                        .forPattern(graph, pattern, window, scenario);
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
                    int vidx = graph.index.stopVertexForStop.get(stop).getIndex();
                    int stopIndex = indexForStop.get(vidx);
                    if (stopIndex == -1) {
                        stopIndex = indexForStop.size();
                        indexForStop.put(vidx, stopIndex);
                        stopForIndex.add(vidx);
                        stopNames.add(stop.getName());
                    }
                    stopIndexesForPattern.add(stopIndex);
                }
                stopsForPattern.add(stopIndexesForPattern.toArray());
            }
        }

        // and do roughly the same thing for added patterns
        if (scenario != null && scenario.modifications != null) {
            for (AddTripPattern atp : Iterables.filter(scenario.modifications, AddTripPattern.class)) {
                // note that added trip patterns are not affected by modifications
                RaptorWorkerTimetable timetable = RaptorWorkerTimetable.forAddedPattern(atp, window);
                if (timetable == null)
                    continue;

                timetablesForPattern.add(timetable);

                // TODO: patternForIndex, indexForPattern

                patternNames.add(atp.name);
                stopsForPattern.add(Arrays.asList(atp.temporaryStops).stream()
                        .mapToInt(t -> t.index)
                        .toArray());
            }
        }

        // create the mapping from stops to patterns
        TIntObjectMap<TIntList> patternsForStopList = new TIntObjectHashMap<>();
        for (int pattern = 0; pattern < stopsForPattern.size(); pattern++) {
            for (int stop : stopsForPattern.get(pattern)) {
                if (!patternsForStopList.containsKey(stop))
                    patternsForStopList.put(stop, new TIntArrayList());

                patternsForStopList.get(stop).add(pattern);
            }
        }

        for (int stop = 0; stop < stopForIndex.size(); stop++) {
            patternsForStop.add(patternsForStopList.get(stop).toArray());
        }

        /** Record transfers between all used stops. */
        for (int stop : stopForIndex) {
            TIntList transfers = new TIntArrayList();
            TransitStop tstop = graph.index.stopVertexForStop.get(stop);
            for (SimpleTransfer simpleTransfer : Iterables.filter(tstop.getOutgoing(), SimpleTransfer.class)) {
                int targetStopIndex = indexForStop.get(simpleTransfer.getToVertex().getIndex());
                if (targetStopIndex != -1) {
                    transfers.add(targetStopIndex);
                    transfers.add((int)(simpleTransfer.getDistance()));
                }
            }
            transfersForStop.add(transfers.toArray());
        }

        StopTreeCache stc = graph.index.getStopTreeCache();

        // Record distances to nearby intersections for all used stops.
        // This is just a copy of StopTreeCache using int indices for stops.
        if (sampleSet == null) {
            for (Stop stop : stopForIndex) {
                TransitStop tstop = graph.index.stopVertexForStop.get(stop);
                targetsForStop.add(stc.distancesForStop.get(tstop));
            }
            nTargets = Vertex.getMaxIndex();
        }

        // Record distances to each sample
        // We need to propagate all the way to samples when doing repeated RAPTOR.
        // Consider the situation where there are two parallel transit lines on
        // 5th Street and 6th Street, and you live on A Street halfway between 5th and 6th.
        // Both lines run at 30 minute headways, but they are exactly out of phase, and for the
        // purposes of this conversation both go the same place with the same in-vehicle travel time.
        // Thus, even though the lines run every 30 minutes, you never experience a wait of more than
        // 15 minutes because you are clever when you choose which line to take. The worst case at each
        // transit stop is much worse than the worst case at samples. While unlikely, it is possible that
        // a sample would be able to reach these two stops within the walk limit, but that the two
        // intersections it is connected to cannot reach both.
        else {
            TIntObjectMap<List<HalfSample>> sampleIndex = new TIntObjectHashMap<List<HalfSample>>();

            for (int i = 0; i < sampleSet.pset.capacity; i++) {
                if (sampleSet.v0s[i] == null)
                    continue;

                // VERTEX 0
                int v0 = sampleSet.v0s[i].getIndex();

                List<HalfSample> list;
                if (sampleIndex.containsKey(v0))
                    list = sampleIndex.get(v0);
                else {
                    list = new ArrayList<HalfSample>();
                    sampleIndex.put(v0, list);
                }

                list.add(new HalfSample(i, sampleSet.d0s[i]));

                // VERTEX 1
                if (sampleSet.v1s[i] != null) {
                    int v1 = sampleSet.v1s[i].getIndex();
                    if (sampleIndex.containsKey(v1))
                        list = sampleIndex.get(v1);
                    else {
                        list = new ArrayList<HalfSample>();
                        sampleIndex.put(v1, list);
                    }

                    list.add(new HalfSample(i, sampleSet.d1s[i]));
                }
            }

            // Iterate over all stops, saving an array of distances to samples from each stop.
            TIntList out = new TIntArrayList();

            STOP: for (Stop stop : stopForIndex) {
                TransitStop tstop = graph.index.stopVertexForStop.get(stop);

                out.clear();

                int[] distancesForStop = stc.distancesForStop.get(tstop);

                STREET: for (int i = 0; i < distancesForStop.length; i++) {
                    int v = distancesForStop[i++];
                    int d = distancesForStop[i];

                    if (!sampleIndex.containsKey(v))
                        continue STREET;

                    // Possible optimization: it's possible (indeed, likely) that the sample
                    // is reachable two ways from a given stop. We could collapse this array down
                    // and make propagation faster.
                    SAMPLE: for (HalfSample s : sampleIndex.get(v)) {
                        int distance = Math.round(d + s.distance);
                        if (distance > stc.maxWalkMeters)
                            continue;

                        out.add(s.index);
                        out.add(distance);
                    }
                }
                // Save a flat array of (target, distance) pairs keyed on this transit stops's index in the RAPTOR table.
                targetsForStop.add(out.toArray());
            }

            nTargets = sampleSet.pset.capacity;
        }

        nStops = stopForIndex.size();
        nPatterns = patternForIndex.size();
    }

    /** find stops near a given location */
    public static TIntIntMap findStopsNear (RoutingRequest rr, Collection<AddTripPattern.TemporaryStop> additionalStops) {
        AStar astar = new AStar();
        rr.longDistance = true;
        rr.setNumItineraries(1);

        Graph graph = rr.rctx.graph;

        ShortestPathTree spt = astar.getShortestPathTree(rr, 5); // timeout in seconds

        TIntIntMap accessTimes = new TIntIntHashMap();

        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                // note that we calculate the time based on the walk speed here rather than
                // based on the time. this matches what we do in the stop tree cache.
                // TODO hardwired walk speed
                accessTimes.put(tstop.getIndex(), (int) (s.getWalkDistance() / 1.3f));
            }
        }

        // and handle the additional stops
        for (AddTripPattern.TemporaryStop tstop: additionalStops) {
            if (tstop.sample == null) {
                LOG.warn("Temporary stop unlinked: {}", tstop);
                continue;
            }

            double dist = Double.POSITIVE_INFINITY;

            if (tstop.sample.v0 != null) {
                State s0 = spt.getState(tstop.sample.v0);

                if (s0 != null) {
                    dist = s0.getWalkDistance() + tstop.sample.d0;
                }
            }

            if (tstop.sample.v1 != null) {
                State s1 = spt.getState(tstop.sample.v1);

                if (s1 != null) {
                    dist = Math.min(s1.getWalkDistance() + tstop.sample.d1, dist);
                }
            }

            if (Double.isInfinite(dist))
                continue;

            // TODO hardwired walk speed
            accessTimes.put(tstop.index, (int) (dist / 1.3f));
        }

        rr.cleanup();
        return accessTimes;
    }

    /** half a sample: the index in the sample set, and the distance to one of the vertices */
    private static class HalfSample {
        public HalfSample(int index, float distance) {
            this.index = index;
            this.distance = distance;
        }

        int index;
        float distance;
    }
}
