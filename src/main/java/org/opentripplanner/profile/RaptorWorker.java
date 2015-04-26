package org.opentripplanner.profile;

import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class RaptorWorker {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorWorker.class);
    private static final int UNREACHED = Integer.MAX_VALUE;
    static final int MAX_ROUNDS = 8;
    static final int MAX_DURATION = 90 * 60;

    int max_time = 0;
    int round = 0;
    List<int[]> timesPerStopPerRound;
    int[] timesPerStop;
    int[] bestTimes;
    int[] transferResults;
    RaptorWorkerData data;

    BitSet stopsTouched;
    BitSet patternsTouched;

    public RaptorWorker(RaptorWorkerData data) {
        this.data = data;
        this.bestTimes = new int[data.nStops];
        stopsTouched = new BitSet(data.nStops);
        patternsTouched = new BitSet(data.nPatterns);
        Arrays.fill(bestTimes, UNREACHED); // initialize once here and reuse on subsequent iterations.
    }

    public void advance () {
        round++;
//        timesPerStop = new int[data.nStops];
//        Arrays.fill(timesPerStop, UNREACHED);
//        timesPerStopPerRound.add(timesPerStop);
    }

    public PropagatedTimesStore runRaptor (Graph graph, TObjectIntMap<TransitStop> accessTimes) {
        long beginCalcTime = System.currentTimeMillis();
        long totalPropagationTime = 0;
        TIntIntMap initialStops = new TIntIntHashMap();
        TObjectIntIterator<TransitStop> initialIterator = accessTimes.iterator();
        while (initialIterator.hasNext()) {
            initialIterator.advance();
            TransitStop tstop = initialIterator.key();
            int accessTime = initialIterator.value();
            int stopIndex = data.indexForStop.get(tstop.getStop());
            if (stopIndex == -1) {
                continue; // stop not used;
            }
            initialStops.put(stopIndex, accessTime);
        }
        PropagatedTimesStore propagatedTimesStore = new PropagatedTimesStore(graph);
        // TIntIntMap[] timesPerMinute = new TIntIntHashMap[60];
        // Iterate backward through minutes (range-raptor) taking a snapshot of router state after each call
        Contiguous2DIntArray vTimes = new Contiguous2DIntArray(60, StreetVertex.getMaxIndex());
        for (int departureTime = 9 * 60 * 60 - 60, n = 0; departureTime >= 8 * 60 * 60; departureTime -= 60, n++) {
            if (n % 15 == 0) {
                LOG.info("minute {}", n);
            }
            this.runRaptor(initialStops, departureTime);
//            for (int s = 0; s < data.nStops; s++) {
//                if (bestTimes[s] != UNREACHED) {
//                    LOG.info("{} {}", bestTimes[s] / 60, data.stopNames.get(s));
//                }
//            }
            // At this point we should have the best travel times possible. Propagate to the streets.
//            int[] timesOnStreets = new int[StreetVertex.getMaxIndex()];

            long beginPropagationTime = System.currentTimeMillis();
            //int[] timesOnStreets = new int[StreetVertex.getMaxIndex()];
            for (int s = 0; s < data.nStops; s++) {
                int baseTimeSeconds = bestTimes[s];
                if (baseTimeSeconds != UNREACHED) {
                    baseTimeSeconds -= departureTime; // convert to travel time rather than clock time
                    // LOG.info("{} {}", baseTimeSeconds / 60, data.stopNames.get(s));
                    int[] targets = data.targetsForStop.get(s);
                    for (int i = 0; i < targets.length; i++) {
                        int streetVertexIndex = targets[i++]; // increment i after read
                        int distance = targets[i]; // i will be incremented at loop end
                        // distance in meters over walk speed in meters per second --> seconds
                        int egressWalkTimeSeconds = distance;
                        int propagated_time = baseTimeSeconds + egressWalkTimeSeconds;
                        vTimes.setIfLess(n, streetVertexIndex, propagated_time);
                    }
                }
            }
            totalPropagationTime += (System.currentTimeMillis() - beginPropagationTime);
        }
        long calcTime = System.currentTimeMillis() - beginCalcTime;
        LOG.info("calc time {}sec", calcTime / 1000.0);
        LOG.info("  propagation {}sec", totalPropagationTime / 1000.0);
        LOG.info("  raptor {}sec", (calcTime - totalPropagationTime) / 1000.0);
////        int nonZero = 0;
////        for (int i = 0; i < timesPerMinute.length; i++) {
////            int[] times = new int[StreetVertex.getMaxIndex()];
////            for (int j = 0; j < timesPerMinute[i].size(); j++) {
////                times[j] = timesPerMinute[i].get(j);
////            }
////            propagatedTimesStore.mergeIn(times);
////        }
//        LOG.info("{}", nonZero); // thwart JIT
        vTimes.dumpVariableByte();
        propagatedTimesStore.setFromBytes(vTimes);
        return propagatedTimesStore;
    }

    public void runRaptor (TIntIntMap initialStops, int departureTime) {
        // Arrays.fill(bestTimes, UNREACHED); hold on to old state
        max_time = departureTime + MAX_DURATION;
        round = 0;
        advance(); // go to first round
        patternsTouched.clear(); // clear patterns left over from previous calls.
        // Copy initial stops over to the first round
        TIntIntIterator iterator = initialStops.iterator();
        while (iterator.hasNext()) {
            iterator.advance();
            int stopIndex = iterator.key();
            int time = iterator.value() + departureTime;
            bestTimes[stopIndex] = time;
            markPatternsForStop(stopIndex);
        }
        // Anytime a round updates some stops, move on to another round
        while (round <= MAX_ROUNDS && doOneRound()) {
            advance();
        }
    }

    public boolean doOneRound () {
        // LOG.info("round {}", round);
        stopsTouched.clear(); // clear any stops left over from previous round.
        for (int p = patternsTouched.nextSetBit(0); p >= 0; p = patternsTouched.nextSetBit(p+1)) {
            // LOG.info("pattern {} {}", p, data.patternNames.get(p));
            int onTrip = -1;
            RaptorWorkerTimetable timetable = data.timetablesForPattern.get(p);
            int[] stops = data.stopsForPattern.get(p);
            int stopPositionInPattern = -1; // first increment will land this at zero
            for (int stopIndex : stops) {
                stopPositionInPattern += 1;
                if (onTrip == -1) {
                    // We haven't boarded yet
                    if (bestTimes[stopIndex] == UNREACHED) {
                        continue; // we've never reached this stop, we can't board.
                    }
                    // Stop has been reached before. Attempt to board here.
                    onTrip = timetable.findDepartureAfter(stopPositionInPattern, bestTimes[stopIndex]);
                    continue; // boarded or not, we move on to the next stop in the sequence
                } else {
                    // We're on board a trip.
                    int arrivalTime = timetable.getArrival(onTrip, stopPositionInPattern);
                    if (arrivalTime < max_time && arrivalTime < bestTimes[stopIndex]) {
                        bestTimes[stopIndex] = arrivalTime;
                        stopsTouched.set(stopIndex);
                    }
                    // Check whether we can back up to an earlier trip.
                    while (onTrip > 0) {
                        int departureOnPreviousTrip = timetable.getDeparture(onTrip - 1, stopPositionInPattern);
                        if (departureOnPreviousTrip > bestTimes[stopIndex]) {
                            onTrip--;
                        } else {
                            break;
                        }
                    }
                }
            }
        }
        doTransfers();
        return !patternsTouched.isEmpty();
    }

    /**
     * Apply transfers.
     * Mark all the patterns passing through these stops and any stops transferred to.
     */
    private void doTransfers() {
        patternsTouched.clear();
        for (int stop = stopsTouched.nextSetBit(0); stop >= 0; stop = stopsTouched.nextSetBit(stop + 1)) {
            markPatternsForStop(stop);
            int fromTime = bestTimes[stop];
            int[] transfers = data.transfersForStop.get(stop);
            for (int i = 0; i < transfers.length; i++) {
                int toStop = transfers[i++]; // increment i
                int distance = transfers[i]; // i will be incremented at the end of the loop
                int toTime = fromTime + distance; // * 1.33
                if (toTime < max_time && toTime < bestTimes[toStop]) {
                    bestTimes[toStop] = toTime;
                    markPatternsForStop(toStop);
                }
             }
        }
    }

    /** Mark all the patterns passing through the given stop. */
    private void markPatternsForStop(int stop) {
        int[] patterns = data.patternsForStop.get(stop);
        for (int pattern : patterns) {
            patternsTouched.set(pattern);
        }
    }

}
