package org.opentripplanner.profile;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Multimap;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;
import gnu.trove.map.hash.TObjectIntHashMap;
import org.joda.time.LocalDate;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.analyst.SampleSet;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.analyst.scenario.AddTripPattern;
import org.opentripplanner.analyst.scenario.ConvertToFrequency;
import org.opentripplanner.analyst.scenario.Scenario;
import org.opentripplanner.analyst.scenario.TransferRule;
import org.opentripplanner.analyst.scenario.TripPatternFilter;
import org.opentripplanner.common.model.GenericLocation;
import org.opentripplanner.routing.algorithm.AStar;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.spt.DominanceFunction;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RaptorWorkerData implements Serializable {
    public static final Logger LOG = LoggerFactory.getLogger(RaptorWorkerData.class);

    /** we use empty int arrays for various things, e.g. transfers from isolated stops. They're immutable so only use one */
    public static final int[] EMPTY_INT_ARRAY = new int[0];

    public final int nStops;

    public final int nPatterns;

    /** The number of targets (vertices or samples) */
    public final int nTargets;

    /** For every stop, one pair of ints (targetStopIndex, distanceMeters) for each transfer out of that stop. This uses 0-based stop indices that are specific to RaptorData */
    public final List<int[]> transfersForStop = new ArrayList<>();

    /** A list of pattern indexes passing through each stop, again using Raptor indices. */
    public final List<int[]> patternsForStop = new ArrayList<>();

    /** For each pattern, a 2D array of stoptimes for each trip on the pattern. */
    public List<RaptorWorkerTimetable> timetablesForPattern = new ArrayList<>();

    /** does this RaptorData have any scheduled trips? */
    public boolean hasSchedules = false;

    /** does this RaptorData have any frequency trips? */
    public boolean hasFrequencies = false;
    
    /**
     * Map from stops that do not exist in the graph but only for the duration of this search to their stop indices in the RAPTOR data.
     */
    public TObjectIntMap<AddTripPattern.TemporaryStop> addedStops = new TObjectIntHashMap<>();

    /** Some stops may have special transfer rules. They are stored here. */
    public TIntObjectMap<List<TransferRule>> transferRules = new TIntObjectHashMap<>();

    /** The transfer rules to be applied in the absence of another transfer rule */
    public List<TransferRule> baseTransferRules = new ArrayList<>();

    /** The boarding assumption used for initial vehicle boarding, and for when there is no transfer rule defined */
    public RaptorWorkerTimetable.BoardingAssumption boardingAssumption;

    /**
     * For each stop, one pair of ints (targetID, distanceMeters) for each destination near that stop.
     * For generic TimeSurfaces these are street intersections. They could be anything though since the worker doesn't
     * care what the IDs stand for. For example, they could be point indexes in a pointset.
     */
    public final List<int[]> targetsForStop = new ArrayList<>();

    /** The 0-based RAPTOR indices of each stop from their vertex IDs */
    public transient final TIntIntMap indexForStop;
     /** Optional debug data: the name of each stop. */
    public transient final List<String> stopNames = new ArrayList<>();
    public transient final List<String> patternNames = new ArrayList<>();

    /** Create RaptorWorkerData for the given window and graph */
    public RaptorWorkerData (Graph graph, TimeWindow window, ProfileRequest request, TaskStatistics ts) {
        this(graph, window, request, null, ts);
    }

    public RaptorWorkerData (Graph graph, TimeWindow window, ProfileRequest request, SampleSet sampleSet) {
        this(graph, window, request, sampleSet, new TaskStatistics());
    }

    public RaptorWorkerData (Graph graph, TimeWindow window, ProfileRequest request) {
        this (graph, window, request, null, new TaskStatistics());
    }

    /** Create RaptorWorkerData to be used to build ResultSets directly without creating an intermediate SampleSet */
    public RaptorWorkerData (Graph graph, TimeWindow window, ProfileRequest req, SampleSet sampleSet, TaskStatistics ts) {
        Scenario scenario = req.scenario;

        int totalPatterns = graph.index.patternForId.size();
        int totalStops = graph.index.stopForId.size();
        timetablesForPattern = new ArrayList<RaptorWorkerTimetable>(totalPatterns);
        List<TripPattern> patternForIndex = Lists.newArrayList(totalPatterns);
        TObjectIntMap<TripPattern> indexForPattern = new TObjectIntHashMap<>(totalPatterns, 0.75f, -1);
        indexForStop = new TIntIntHashMap(totalStops, 0.75f, Integer.MIN_VALUE, -1);
        TIntList stopForIndex = new TIntArrayList(totalStops, Integer.MIN_VALUE);

        this.boardingAssumption = req.boardingAssumption;

        ts.patternCount = 0;
        ts.frequencyEntryCount = 0;
        ts.frequencyTripCount = 0;
        ts.scheduledTripCount = 0;

        // first apply any filters that need to be applied to the entire schedule at once
        Collection<TripPattern> graphPatterns = graph.index.patternForId.values();

        // convert scheduled trips to freuquencies as needed
        if (scenario != null && scenario.modifications != null) {
            Collection<ConvertToFrequency> frequencies = scenario.modifications.stream()
                    .filter(m -> m instanceof ConvertToFrequency)
                    .map(m -> (ConvertToFrequency) m)
                    .collect(Collectors.toList());

            if (!frequencies.isEmpty()) {
                // apply the operations
                List<TripTimes> scheduled = graphPatterns.stream()
                        .flatMap(p -> p.scheduledTimetable.tripTimes.stream())
                        .collect(Collectors.toList());

                List<FrequencyEntry> frequencyEntries = graphPatterns.stream()
                        .filter(p -> p.getSingleFrequencyEntry() != null)
                        .flatMap(p -> p.scheduledTimetable.frequencyEntries.stream())
                        .collect(Collectors.toList());

                for (ConvertToFrequency c : frequencies) {
                    c.apply(frequencyEntries, scheduled, graph, window.servicesRunning, req.boardingAssumption);
                    scheduled = c.scheduledTrips;
                    frequencyEntries = c.frequencyEntries;
                }

                // aggregate the modified trips back into patterns
                // this is so that the patterns have object references to the relevant schedules and frequency entries
                // group by the original pattern - each entry is still associated with a trip
                Multimap<TripPattern, TripTimes> newScheduledPatterns = HashMultimap.create();
                for (TripTimes tt : scheduled) {
                    newScheduledPatterns.put(graph.index.patternForTrip.get(tt.trip), tt);
                }

                Multimap<TripPattern, FrequencyEntry> newFreqEntries = HashMultimap.create();
                for (FrequencyEntry fe : frequencyEntries) {
                    newFreqEntries.put(graph.index.patternForTrip.get(fe.tripTimes.trip), fe);
                }

                // filter to just the patterns that are relevant, then replace each pattern with a new pattern
                // representing its modified trips
                graphPatterns = graphPatterns.stream()
                        .filter(p -> newScheduledPatterns.containsKey(p) || newFreqEntries.containsKey(p))
                        .map(p -> {
                            TripPattern newp = new TripPattern(p.route, p.stopPattern);
                            newScheduledPatterns.get(p).stream().forEach(newp.scheduledTimetable::addTripTimes);
                            newFreqEntries.get(p).stream().forEach(newp.scheduledTimetable::addFrequencyEntry);
                            return newp;
                        })
                        .collect(Collectors.toList());

            }
        }

        /* Make timetables for active trip patterns and record the stops each active pattern uses. */
        for (TripPattern originalPattern : graphPatterns) {
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
                        .forPattern(graph, pattern, window, scenario, ts);
                if (timetable == null) {
                    // Pattern is not running during the time window
                    continue;
                }

                timetable.dataIndex = timetablesForPattern.size();
                timetable.raptorData = this;
                timetablesForPattern.add(timetable);

                if (timetable.hasFrequencyTrips())
                    hasFrequencies = true;

                if (timetable.hasScheduledTrips())
                    hasSchedules = true;

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

                timetable.stopIndices = stopIndexesForPattern.toArray();
            }
        }

        // and do roughly the same thing for added patterns
        if (scenario != null && scenario.modifications != null) {
            for (AddTripPattern atp : Iterables.filter(scenario.modifications, AddTripPattern.class)) {
                // note that added trip patterns are not affected by modifications
                RaptorWorkerTimetable timetable = RaptorWorkerTimetable.forAddedPattern(atp, window, ts);
                if (timetable == null)
                    continue;

                timetable.dataIndex = timetablesForPattern.size();
                timetablesForPattern.add(timetable);
                timetable.raptorData = this;

                if (timetable.hasFrequencyTrips())
                    this.hasFrequencies = true;
                if (timetable.hasScheduledTrips())
                    this.hasSchedules = true;

                // TODO: patternForIndex, indexForPattern

                patternNames.add(atp.name);

                // create the stops for the pattern, and collect the temporary stops
                for (AddTripPattern.TemporaryStop t : atp.temporaryStops) {
                    // the index of this stop in the worker data
                    int stopIndex = stopForIndex.size();
                    addedStops.put(t, stopIndex);
                    indexForStop.put(t.index, stopIndex);
                    stopForIndex.add(t.index);
                }

                timetable.stopIndices = Arrays.asList(atp.temporaryStops).stream()
                        .mapToInt(t -> indexForStop.get(t.index))
                        .toArray();
            }
        }

        // for each of the added stops, compute transfers and a stop tree cache
        // Indexed by vertex ID, not RAPTOR index
        TIntObjectMap<int[]> temporaryStopTreeCache = new TIntObjectHashMap<>();;

        // Holds transfer both from _and_ to temporary stops
        // Indexed by and contains vertex ID, not RAPTOR index
        TIntObjectMap<TIntIntMap> temporaryTransfers = new TIntObjectHashMap<>();

        AStar astar = new AStar();
        for (AddTripPattern.TemporaryStop t : addedStops.keySet()) {
            // forward search: stop tree cache and transfers out
            RoutingRequest rr = new RoutingRequest(TraverseMode.WALK);
            rr.batch = true;
            rr.from = rr.to = new GenericLocation(t.lat, t.lon);
            try {
                rr.setRoutingContext(graph);
            } catch (VertexNotFoundException vnfe) {
                LOG.warn("Temporary stop at {}, {} not connected to graph", t.lat, t.lon);
                temporaryStopTreeCache.put(t.index, new int[0]);
                temporaryTransfers.put(t.index, new TIntIntHashMap());
                continue;
            }
            // We use walk-distance limiting and a least-walk dominance function in order to be consistent with egress walking
            // which is implemented this way because walk times can change when walk speed changes. Also, walk times are floating
            // point and can change slightly when streets are split. Street lengths are internally fixed-point ints, which do not
            // suffer from roundoff. Great care is taken when splitting to preserve sums.
            // When cycling, this is not an issue; we already have an explicitly asymmetrical search (cycling at the origin, walking at the destination),
            // so we need not preserve symmetry.
            rr.maxWalkDistance = 2000;
            rr.softWalkLimiting = false;
            rr.dominanceFunction = new DominanceFunction.LeastWalk();
            rr.longDistance = true;
            rr.numItineraries = 1;

            ShortestPathTree spt = astar.getShortestPathTree(rr, 5);

            // create the stop tree cache
            // TODO duplicated code from stopTreeCache
            // Copy vertex indices and distances into a flattened 2D array
            int[] distances = new int[spt.getVertexCount() * 2];
            int i = 0;
            for (Vertex vertex : spt.getVertices()) {
                State state = spt.getState(vertex);

                if (state == null)
                    continue;

                distances[i++] = vertex.getIndex();
                distances[i++] = (int) state.getWalkDistance();
            }
            temporaryStopTreeCache.put(t.index, distances);

            // NB using walk speed of 1 m/s here since we want meters, not seconds
            // walk speed is applied during search.
            TIntIntMap transfersFromStop = findStopsNear(spt, graph, false, 1f);

            // convert it to use indices in the graph not in the worker data
            TIntIntMap transfersFromStopWithGraphIndices = new TIntIntHashMap();

            for (TIntIntIterator trIt = transfersFromStop.iterator(); trIt.hasNext();) {
                trIt.advance();
                transfersFromStopWithGraphIndices.put(stopForIndex.get(trIt.key()), trIt.value());
            }

            temporaryTransfers.put(t.index, transfersFromStopWithGraphIndices);

            rr.cleanup();

            // now compute transfers to the stop by doing a batch arrive-by search
            // necessary to hand-clear routing context so that it is not cached
            rr.rctx = null;
            rr.setArriveBy(true);

            // reset routing context because temporary edges face the wrong way
            rr.setRoutingContext(graph);
            spt = astar.getShortestPathTree(rr, 5);

            // NB using walk speed of 1 m/s here since we want meters, not seconds
            // walk speed is applied during search
            TIntIntMap transfersToStop = findStopsNear(spt, graph, false, 1f);

            // turn the array around; these are transfers from elsewhere to this stop.
            for (TIntIntIterator it = transfersToStop.iterator(); it.hasNext(); ) {
                it.advance();

                // index of vertex in graph
                int graphIndex = stopForIndex.get(it.key());

                // for absolute determinism we want to ensure that transfers between two added stops are always computed
                // in the forward direction. Otherwise they would be computed twice, once forwards
                // and once reverse, and which one was saved would depend on iteration order.
                // OTP should be symmetrical but let's not write code that depends on that property
                Vertex tstop = graph.getVertexById(graphIndex);
                if (tstop == null || !TransitStop.class.isInstance(tstop))
                    // this is not a permanent stop - there is no transit stop vertex associated with it
                    continue;

                if (!temporaryTransfers.containsKey(graphIndex))
                    temporaryTransfers.put(graphIndex, new TIntIntHashMap());

                // temporary transfers are stored by index in the graph not the worker data
                temporaryTransfers.get(graphIndex).put(t.index, it.value());
            }
        }

        // create the mapping from stops to patterns
        TIntObjectMap<TIntList> patternsForStopList = new TIntObjectHashMap<>();
        for (int pattern = 0; pattern < timetablesForPattern.size(); pattern++) {
            for (int stop : timetablesForPattern.get(pattern).stopIndices) {
                if (!patternsForStopList.containsKey(stop))
                    patternsForStopList.put(stop, new TIntArrayList());

                patternsForStopList.get(stop).add(pattern);
            }
        }

        for (int stop = 0; patternsForStopList.containsKey(stop); stop++) {
            patternsForStop.add(patternsForStopList.get(stop).toArray());
        }

        /** Record transfers between all used stops. */
        for (TIntIterator it = stopForIndex.iterator(); it.hasNext();) {
            int stop = it.next();
            TIntList transfers = new TIntArrayList();
            TransitStop tstop = (TransitStop) graph.getVertexById(stop);

            if (tstop != null) {
                // not an added stop, look for transfers in the graph
                for (SimpleTransfer simpleTransfer : Iterables
                        .filter(tstop.getOutgoing(), SimpleTransfer.class)) {
                    int targetStopIndex = indexForStop.get(simpleTransfer.getToVertex().getIndex());
                    if (targetStopIndex != -1) {
                        transfers.add(targetStopIndex);
                        transfers.add((int) (simpleTransfer.getDistance()));
                    }
                }
            }

            // check for any transfers to/from added stops
            if (temporaryTransfers.containsKey(stop)) {
                for (TIntIntIterator tranIt = temporaryTransfers.get(stop).iterator(); tranIt.hasNext();) {
                    tranIt.advance();
                    // stop index
                    transfers.add(indexForStop.get(tranIt.key()));
                    // distance
                    transfers.add(tranIt.value());
                }
            }

            if (!transfers.isEmpty())
                transfersForStop.add(transfers.toArray());
            else
                transfersForStop.add(EMPTY_INT_ARRAY);
        }

        long stcStart = System.currentTimeMillis();
        StopTreeCache stc = graph.index.getStopTreeCache();
        ts.stopTreeCaching = (int) (System.currentTimeMillis() - stcStart);

        // Record times to nearby intersections for all used stops.
        // We use times rather than distances to avoid a costly floating-point divide during propagation
        if (sampleSet == null) {
            int maxWalkDistance = (int) (req.maxWalkTime * 60 * req.walkSpeed);
            for (TIntIterator stopIt = stopForIndex.iterator(); stopIt.hasNext();) {
                int stop = stopIt.next();

                // permanent stop
                Vertex tstop = graph.getVertexById(stop);
                boolean isPermanentStop = tstop != null && TransitStop.class.isInstance(tstop);
                // convert distance to time
                int[] distancesForStop = isPermanentStop ? stc.distancesForStop.get(tstop) : temporaryStopTreeCache.get(stop);
                TIntList timesForStop = new TIntArrayList();

                for (int i = 0; i < distancesForStop.length; i += 2) {
                    int vidx = distancesForStop[i];
                    int dist = distancesForStop[i + 1];

                    // only add if it's less than the max walk distance
                    if (dist <= maxWalkDistance) {
                        timesForStop.add(vidx);
                        // convert meters to seconds by dividing by meters / second
                        timesForStop.add((int) (dist / req.walkSpeed));
                    }
                }

                targetsForStop.add(timesForStop.toArray());
            }

            // TODO memory leak when many graphs have been built
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

            // Iterate over all stops, saving the least distance to each reachable sample from
            // from each transit stop.
            // We first make a map stops to samples, so we can ensure we save only the shortest
            // distance from a transit stop to a sample. Most transit stops can reach a given sample two
            // ways because they can reach both of the vertices the sample is connected to.
            TIntIntMap out = new TIntIntHashMap();

            for (TIntIterator stopIt = stopForIndex.iterator(); stopIt.hasNext();) {
                out.clear();

                int stop = stopIt.next();

                int[] distancesForStop;
                
                Vertex tstop = graph.getVertexById(stop);
                if (tstop != null && TransitStop.class.isInstance(tstop))
                    // permanent stop
                    distancesForStop = stc.distancesForStop.get(tstop);
                else
                    // temporary stop
                    distancesForStop = temporaryStopTreeCache.get(stop);

                STREET: for (int i = 0; i < distancesForStop.length; i++) {
                    int v = distancesForStop[i++];
                    int d = distancesForStop[i];

                    if (!sampleIndex.containsKey(v))
                        continue STREET;

                    // Build the map
                    SAMPLE: for (HalfSample s : sampleIndex.get(v)) {
                        int distance = Math.round(d + s.distance);
                        if (distance > stc.maxWalkMeters)
                            continue;

                        // only save it if there isn't another shorter walking path that we've already encountered.
                        int time = (int) (distance / req.walkSpeed);
                        if (!out.containsKey(s.index) || out.get(s.index) > time)
                            out.put(s.index, time);
                    }
                }
                // Save a flat array of (target, distance) pairs keyed on this transit stops's index in the RAPTOR table.
                int[] flat = new int[out.size() * 2];

                int pos = 0;
                for (TIntIntIterator it = out.iterator(); it.hasNext();) {
                    it.advance();
                    flat[pos++] = it.key();
                    flat[pos++] = it.value();
                }

                targetsForStop.add(flat);
            }

            nTargets = sampleSet.pset.capacity;
        }

        // store transfer rules by stop
        if (scenario != null && scenario.modifications != null) {
            for (TransferRule tr : Iterables.filter(scenario.modifications, TransferRule.class)) {
                if (tr.stop == null) {
                    this.baseTransferRules.add(tr);
                }
                else {
                    Vertex tstop = graph.getVertex(tr.stop);

                    if (tstop == null || !TransitStop.class.isInstance(tstop))
                        LOG.warn("Transit stop not found for transfer rule with stop label {}", tr.stop);

                    if (!indexForStop.containsKey(tstop.getIndex()))
                        // this stop is not used in this time window
                        continue;

                    int index = indexForStop.get(tstop.getIndex());

                    if (!transferRules.containsKey(index))
                        transferRules.put(index, new ArrayList<>());

                    transferRules.get(index).add(tr);
                }
            }
        }

        ts.stopCount = nStops = stopForIndex.size();
        ts.patternCount = nPatterns = timetablesForPattern.size();
        ts.targetCount = nTargets;
    }

    /** find stops from a given SPT, including temporary stops. If useTimes is true, use times from the SPT, otherwise use distances */
    public TIntIntMap findStopsNear (ShortestPathTree spt, Graph graph, boolean useTimes, float walkSpeed) {
        TIntIntMap accessTimes = new TIntIntHashMap();

        for (TransitStop tstop : graph.index.stopVertexForStop.values()) {
            State s = spt.getState(tstop);
            if (s != null) {
                // note that we calculate the time based on the walk speed here rather than
                // based on the time. this matches what we do in the stop tree cache.
                int stopIndex = indexForStop.get(tstop.getIndex());
                
                if (stopIndex != -1) {
                    if (useTimes)
                        accessTimes.put(stopIndex, (int) s.getElapsedTimeSeconds());
                    else
                        accessTimes.put(stopIndex, (int) (s.getWalkDistance() / walkSpeed));
                }
            }
        }

        // and handle the additional stops
        for (TObjectIntIterator<AddTripPattern.TemporaryStop> it = addedStops.iterator(); it.hasNext();) {
            it.advance();
            
            AddTripPattern.TemporaryStop tstop = it.key(); 
            if (tstop.sample == null) {
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
                    double d1 = s1.getWalkDistance() + tstop.sample.d1;
                    dist = Double.isInfinite(dist) ? d1 : Math.min(d1, dist);
                }
            }

            if (Double.isInfinite(dist))
                continue;

            // NB using the index in the worker data not the index in the graph!
            accessTimes.put(it.value(), (int) (dist / walkSpeed));
        }

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
