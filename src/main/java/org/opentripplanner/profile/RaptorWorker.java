package org.opentripplanner.profile;

import com.google.protobuf.CodedOutputStream;
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

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

public class RaptorWorker {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorWorker.class);
    public static final int UNREACHED = Integer.MAX_VALUE;
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
    
    private ProfileRequest req;

    public RaptorWorker(RaptorWorkerData data, ProfileRequest req) {
        this.data = data;
        this.bestTimes = new int[data.nStops];
        stopsTouched = new BitSet(data.nStops);
        patternsTouched = new BitSet(data.nPatterns);
        this.req = req; 
        Arrays.fill(bestTimes, UNREACHED); // initialize once here and reuse on subsequent iterations.
    }

    public void advance () {
        round++;
//        timesPerStop = new int[data.nStops];
//        Arrays.fill(timesPerStop, UNREACHED);
//        timesPerStopPerRound.add(timesPerStop);
    }

    public PropagatedTimesStore runRaptor (Graph graph, TObjectIntMap<TransitStop> accessTimes, int[] walkTimes) {
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
        
        int iterations = (req.toTime - req.fromTime - 60) / 60 + 1;
        
        // Iterate backward through minutes (range-raptor) taking a snapshot of router state after each call
        int[][] timesAtTargetsEachMinute = new int[iterations][walkTimes.length];
        
        for (int departureTime = req.toTime - 60, n = 0; departureTime >= req.fromTime; departureTime -= 60, n++) {
            if (n % 15 == 0) {
                LOG.info("minute {}", n);
            }
            this.runRaptor(initialStops, departureTime);
            long beginPropagationTime = System.currentTimeMillis();
            int[] timesAtTargets = timesAtTargetsEachMinute[n];

            System.arraycopy(walkTimes, 0, timesAtTargets, 0, walkTimes.length);
            
            for (int i = 0; i < timesAtTargets.length; i++) {
            	timesAtTargets[i] = walkTimes[i];
            }
            
            for (int s = 0; s < data.nStops; s++) {
            	// it's safe to use the best time at this stop for any number of transfers, even in range-raptor,
            	// because we allow unlimited transfers. this is slightly different from the original RAPTOR implementation:
            	// we do not necessarily compute all pareto-optimal paths on (journey time, number of transfers)
                int baseTimeSeconds = bestTimes[s];
                if (baseTimeSeconds != UNREACHED) {
                    baseTimeSeconds -= departureTime; // convert to travel time rather than clock time
                    int[] targets = data.targetsForStop.get(s);
                    for (int i = 0; i < targets.length; i++) {
                        int targetIndex = targets[i++]; // increment i after read
                        int distance = targets[i]; // i will be incremented at loop end
                        // distance in meters over walk speed in meters per second --> seconds
                        int egressWalkTimeSeconds = (int) (distance / req.walkSpeed);
                        int propagated_time = baseTimeSeconds + egressWalkTimeSeconds;
                        if (timesAtTargets[targetIndex] > propagated_time) {
                            timesAtTargets[targetIndex] = propagated_time;
                        }
                    }
                }
            }
            totalPropagationTime += (System.currentTimeMillis() - beginPropagationTime);
        }
        long calcTime = System.currentTimeMillis() - beginCalcTime;
        LOG.info("calc time {}sec", calcTime / 1000.0);
        LOG.info("  propagation {}sec", totalPropagationTime / 1000.0);
        LOG.info("  raptor {}sec", (calcTime - totalPropagationTime) / 1000.0);
        //dumpVariableByte(timesAtTargetsEachMinute);
        propagatedTimesStore.setFromArray(timesAtTargetsEachMinute);
        return propagatedTimesStore;
    }

    public void dumpVariableByte(int[][] array) {
        try {
            FileOutputStream fos = new FileOutputStream("/Users/abyrd/results.dat");
            CodedOutputStream cos = CodedOutputStream.newInstance(fos);
            cos.writeUInt32NoTag(array.length);
            for (int[] subArray : array) {
                cos.writeUInt32NoTag(subArray.length);
                for (int val : subArray) {
                    cos.writeInt32NoTag(val);
                }
            }
            fos.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
                int toTime = fromTime + (int) (distance / req.walkSpeed); // * 1.33
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
