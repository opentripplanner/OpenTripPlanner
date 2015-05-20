package org.opentripplanner.profile;

import com.google.protobuf.CodedOutputStream;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.iterator.TObjectIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;

/**
 * A RaptorWorker carries out RAPTOR searches on a pre-filtered, compacted representation of all the trips running
 * during a given time window. It originated as a rewrite of our RAPTOR code that would use "thin workers", allowing
 * computation by a generic function-execution service like AWS Lambda. The gains in efficiency were significant enough
 * that RaptorWorkers are now used in the context of a full-size OTP server executing spatial analysis tasks.
 *
 * We can imagine that someday all OTP searches, including simple point-to-point searches, may be carried out on such
 * compacted tables, including both the transit and street searches (via a compacted street network in a column store
 * or struct-like byte array using something like the FastSerialization library).
 *
 * This implements the RAPTOR algorithm; see http://research.microsoft.com/pubs/156567/raptor_alenex.pdf
 */
public class RaptorWorker {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorWorker.class);
    public static final int UNREACHED = Integer.MAX_VALUE;
    static final int MAX_DURATION = 120 * 60;

    int max_time = 0;
    int round = 0;
    List<int[]> timesPerStopPerRound;
    int[] timesPerStop;
    int[] bestTimes;

    /** The best times for reaching stops via transit rather than via a transfer from another stop */
    int[] bestNonTransferTimes;
    int[] transferResults;
    RaptorWorkerData data;

    BitSet stopsTouched;
    BitSet patternsTouched;

    private ProfileRequest req;

    public RaptorWorker(RaptorWorkerData data, ProfileRequest req) {
        this.data = data;
        this.bestTimes = new int[data.nStops];
        this.bestNonTransferTimes = new int[data.nStops];
        stopsTouched = new BitSet(data.nStops);
        patternsTouched = new BitSet(data.nPatterns);
        this.req = req; 
        Arrays.fill(bestTimes, UNREACHED); // initialize once here and reuse on subsequent iterations.
        Arrays.fill(bestNonTransferTimes, UNREACHED);
    }

    public void advance () {
        round++;
        //        timesPerStop = new int[data.nStops];
        //        Arrays.fill(timesPerStop, UNREACHED);
        //        timesPerStopPerRound.add(timesPerStop);
        // uncomment to disable range-raptor
        //Arrays.fill(bestTimes, UNREACHED);
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

        PropagatedTimesStore propagatedTimesStore = new PropagatedTimesStore(graph, data.nTargets);

        int iterations = (req.toTime - req.fromTime - 60) / 60 + 1;

        // Iterate backward through minutes (range-raptor) taking a snapshot of router state after each call
        int[][] timesAtTargetsEachMinute = new int[iterations][data.nTargets];

        for (int departureTime = req.toTime - 60, n = 0; departureTime >= req.fromTime; departureTime -= 60, n++) {
            if (n % 15 == 0) {
                LOG.info("minute {}", n);
            }
            this.runRaptor(initialStops, departureTime);
            long beginPropagationTime = System.currentTimeMillis();
            int[] timesAtTargets = timesAtTargetsEachMinute[n];

            // Record distances to each sample or intersection
            // We need to propagate all the way to samples (or intersections if there are no samples)
            // when doing repeated RAPTOR.
            // Consider the situation where there are two parallel transit lines on
            // 5th Street and 6th Street, and you live on A Street halfway between 5th and 6th.
            // Both lines run at 30 minute headways, but they are exactly out of phase, and for the
            // purposes of this conversation both go the same place with the same in-vehicle travel time.
            // Thus, even though the lines run every 30 minutes, you never experience a wait of more than
            // 15 minutes because you are clever when you choose which line to take. The worst case at each
            // transit stop is much worse than the worst case at samples. While unlikely, it is possible that
            // a sample would be able to reach these two stops within the walk limit, but that the two
            // intersections it is connected to cannot reach both.

            // We include the walk-only times to access transit in the results so that there are not increases in time
            // to reach blocks around the origin due to being forced to ride transit.
            System.arraycopy(walkTimes, 0, timesAtTargets, 0, walkTimes.length);

            for (int s = 0; s < data.nStops; s++) {
                // it's safe to use the best time at this stop for any number of transfers, even in range-raptor,
                // because we allow unlimited transfers. this is slightly different from the original RAPTOR implementation:
                // we do not necessarily compute all pareto-optimal paths on (journey time, number of transfers).
                int baseTimeSeconds = bestNonTransferTimes[s];
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
            // note not setting bestNonTransferTimes here because the initial walk is effectively a "transfer"
            bestTimes[stopIndex] = Math.min(time, bestTimes[stopIndex]);
            markPatternsForStop(stopIndex);
        }
        // Anytime a round updates some stops, move on to another round
        while (doOneRound()) {
            advance();
        }
    }

    public boolean doOneRound () {
        //LOG.info("round {}", round);
        stopsTouched.clear(); // clear any stops left over from previous round.
        PATTERNS: for (int p = patternsTouched.nextSetBit(0); p >= 0; p = patternsTouched.nextSetBit(p+1)) {
            //LOG.info("pattern {} {}", p, data.patternNames.get(p));
            int onTrip = -1;
            RaptorWorkerTimetable timetable = data.timetablesForPattern.get(p);
            int[] stops = data.stopsForPattern.get(p);
            int stopPositionInPattern = -1; // first increment will land this at zero

            int bestFreqBoardTime = Integer.MAX_VALUE;
            int bestFreqBoardStop = -1;
            int bestFreq = -1;

            // first look for a frequency entry
            for (int stopIndex : stops) {
                stopPositionInPattern += 1;

                // the time at this stop if we remain on board a vehicle we had already boarded
                int remainOnBoardTime;
                if (bestFreq != -1) {
                    // we are already aboard a trip, stay on board
                    remainOnBoardTime = bestFreqBoardTime +
                            timetable.getFrequencyTravelTime(bestFreq, bestFreqBoardStop, stopPositionInPattern);  
                }
                else {
                    // we cannot remain on board as we are not yet on board
                    remainOnBoardTime = Integer.MAX_VALUE;
                }

                // the time at this stop if we board a new vehicle
                if (bestTimes[stopIndex] != UNREACHED) {
                    for (int trip = 0; trip < timetable.getFrequencyTripCount(); trip++) {
                        int boardTime = timetable.getFrequencyDeparture(trip, stopPositionInPattern, bestTimes[stopIndex], true);

                        if (boardTime != -1 && boardTime < remainOnBoardTime) {
                            // make sure we board the best frequency entry at a stop
                            if (bestFreqBoardStop == stopPositionInPattern && bestFreqBoardTime < boardTime)
                                continue;

                            // board this vehicle
                            // note: this boards the trip with the lowest headway at the given time.
                            // if there are overtaking trips all bets are off.
                            bestFreqBoardTime = boardTime;
                            bestFreqBoardStop = stopPositionInPattern;
                            bestFreq = trip;
                            // note that we do not break the loop in case there's another frequency entry that is better
                        }
                    }
                }

                // save the remain on board time. If we boarded a new trip then we know that the
                // remain on board time must be larger than the arrival time at the stop so will
                // not be saved; no need for an explicit check.
                if (remainOnBoardTime != Integer.MAX_VALUE && remainOnBoardTime < max_time) {
                    if (bestNonTransferTimes[stopIndex] > remainOnBoardTime) {
                        bestNonTransferTimes[stopIndex] = remainOnBoardTime;

                        stopsTouched.set(stopIndex);

                        if (bestTimes[stopIndex] > remainOnBoardTime)
                            bestTimes[stopIndex] = remainOnBoardTime;
                    }
                }
            }

            // don't mix frequencies and timetables
            if (bestFreq != -1)
                continue PATTERNS;

            stopPositionInPattern = -1;

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
                    if (arrivalTime < max_time && arrivalTime < bestNonTransferTimes[stopIndex]) {
                        bestNonTransferTimes[stopIndex] = arrivalTime;

                        stopsTouched.set(stopIndex);

                        if (arrivalTime < bestTimes[stopIndex])
                            bestTimes[stopIndex] = arrivalTime;

                    }

                    // Check whether we can back up to an earlier trip. This could be due to an overtaking trip,
                    // or (more likely) because there was a faster way to get to a stop further down the line. 
                    while (onTrip > 0) {
                        int departureOnPreviousTrip = timetable.getDeparture(onTrip - 1, stopPositionInPattern);
                        // use bestTime not bestNonTransferTimes to allow transferring to this trip later on down the route
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
            // TODO this is reboarding every trip at every stop.
            markPatternsForStop(stop);
            int fromTime = bestNonTransferTimes[stop];
            int[] transfers = data.transfersForStop.get(stop);
            for (int i = 0; i < transfers.length; i++) {
                int toStop = transfers[i++]; // increment i
                int distance = transfers[i]; // i will be incremented at the end of the loop
                int toTime = fromTime + (int) (distance / req.walkSpeed);
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
