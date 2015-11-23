package org.opentripplanner.profile;

import com.google.protobuf.CodedOutputStream;
import gnu.trove.iterator.TIntIntIterator;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.routing.graph.Graph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.stream.IntStream;

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

    /**
     * Destinations with travel time above this threshold are considered unreachable. Note that this will cut off half of the
     * trips to a particular location if half the time it takes less than the cutoff and half the time more. Use in combination
     * with the reachability threshold in ProfileRequest to get desired results.
     *
     * The fact that there are edge effects here implies a problem with our methodology. We should be taking the approach that
     * the Accessibility Observatory has taken in which accessibility (rather than travel time) is calculated at each minute.
     * Alternatively, we could calculate the full OD matrix for each minute and save that to determine whether a particular destination
     * is reachable in less than x minutes on median for any arbitrary cutoff.
     *
     * This should be a number beyond which people would generally consider transit to be completely unreasonable.
     */
    static final int MAX_DURATION = 120 * 60;

    /**
     * The number of randomized frequency schedule draws to take for each minute of the search.
     *
     * We loop over all departure minutes and do a schedule search, and then run this many Monte Carlo
     * searches with randomized frequency schedules within each minute. It does not need to be particularly
     * high as it happens each minute, and there is likely a lot of repetition in the scheduled service
     * (i.e. many minutes look like each other), so several minutes' monte carlo draws are effectively
     * pooled.
     */
    public static final int MONTE_CARLO_COUNT_PER_MINUTE = 2;

    /** If there are no schedules, the number of Monte Carlo draws to take */
    public static final int TOTAL_MONTE_CARLO_COUNT = 99;

    int max_time = 0;
    int round = 0;
    List<int[]> timesPerStopPerRound;
    int[] timesPerStop;
    int[] bestTimes;


    /**
     * The previous pattern used to get to this stop, parallel to bestTimes. Used to apply transfer rules. This is conceptually
     * similar to the "parent pointer" used in the RAPTOR paper to allow reconstructing paths. This could
     * be used to reconstruct a path (although potentially not the one that was used to get to a particular
     * location, as a later round may have found a faster but more-transfers way to get there). A path
     * reconstructed this way will tbus be optimal in the earliest-arrival sense but may not have the
     * fewest transfers; in fact, it will tend not to.
     *
     * Consider the case where there is a slower one-seat ride and a quicker route with a transfer
     * to get to a transit center. At the transit center you board another vehicle. If it turns out
     * that you still catch that vehicle at the same time regardless of which option you choose,
     * general utility theory would suggest that you would choose the one seat ride due to a) the
     * inconvenience of the transfer and b) the fact that most people have a smaller disutility for
     * in-vehicle time than waiting time, especially if the waiting is exposed to the elements, etc.
     *
     * However, this implementation will find the more-transfers trip because it doesn't know where you're
     * going from the transit center, whereas true RAPTOR would find both. It's not non-optimal in the
     * earliest arrival sense, but it's also not the only optimal option.
     *
     * All of that said, we could reconstruct paths simply by storing one more parallel array with
     * the index of the stop that you boarded a particular pattern at. Then we can do the typical
     * reverse-optimization step.
     */
    int[] previousPatterns;

    /** The best times for reaching stops via transit rather than via a transfer from another stop */
    int[] bestNonTransferTimes;

    RaptorWorkerData data;

    /** stops touched this round */
    BitSet stopsTouched;

    /** stops touched any round this minute */
    BitSet allStopsTouched;

    BitSet patternsTouched;

    private ProfileRequest req;

    private long totalPropagationTime = 0;

    private FrequencyRandomOffsets offsets;

    public RaptorWorker(RaptorWorkerData data, ProfileRequest req) {
        this.data = data;
        // these should only reflect the results of the (deterministic) scheduled search
        this.bestTimes = new int[data.nStops];
        this.bestNonTransferTimes = new int[data.nStops];
        this.previousPatterns = new int[data.nStops];
        Arrays.fill(previousPatterns, -1);
        allStopsTouched = new BitSet(data.nStops);
        stopsTouched = new BitSet(data.nStops);
        patternsTouched = new BitSet(data.nPatterns);
        this.req = req; 
        Arrays.fill(bestTimes, UNREACHED); // initialize once here and reuse on subsequent iterations.
        Arrays.fill(bestNonTransferTimes, UNREACHED);
        offsets = new FrequencyRandomOffsets(data);
    }

    public void advance () {
        round++;
        //        timesPerStop = new int[data.nStops];
        //        Arrays.fill(timesPerStop, UNREACHED);
        //        timesPerStopPerRound.add(timesPerStop);
        // uncomment to disable range-raptor
        //Arrays.fill(bestTimes, UNREACHED);
    }

    /**
     * @param accessTimes a map from transit stops to the time it takes to reach those stops
     * @param nonTransitTimes the time to reach all targets without transit. Targets can be vertices or points/samples.
     */
    public PropagatedTimesStore runRaptor (Graph graph, TIntIntMap accessTimes, int[] nonTransitTimes, TaskStatistics ts) {
        long beginCalcTime = System.currentTimeMillis();
        TIntIntMap initialStops = new TIntIntHashMap();
        TIntIntIterator initialIterator = accessTimes.iterator();
        while (initialIterator.hasNext()) {
            initialIterator.advance();
            int stopIndex = initialIterator.key();
            int accessTime = initialIterator.value();
            initialStops.put(stopIndex, accessTime);
        }

        PropagatedTimesStore propagatedTimesStore = new PropagatedTimesStore(graph, this.req, data.nTargets);

        // optimization: if no schedules, only run Monte Carlo
        int fromTime = req.fromTime;
        int monteCarloDraws = MONTE_CARLO_COUNT_PER_MINUTE;
        if (!data.hasSchedules) {
            // only do one iteration
            fromTime = req.toTime - 60;
            monteCarloDraws = TOTAL_MONTE_CARLO_COUNT;
        }

        // if no frequencies, don't run Monte Carlo
        int iterations = (req.toTime - fromTime - 60) / 60 + 1;

        // if we do Monte Carlo, we do more iterations. But we only do monte carlo when we have frequencies.
        // So only update the number of iterations when we're actually going to use all of them, to
        // avoid uninitialized arrays.
        // if we multiply when we're not doing monte carlo, we'll end up with too many iterations.
        if (data.hasFrequencies)
            // we add 2 because we do two "fake" draws where we do min or max instead of a monte carlo draw
            iterations *= (monteCarloDraws + 2);

        ts.searchCount = iterations;

        // Iterate backward through minutes (range-raptor) taking a snapshot of router state after each call
        int[][] timesAtTargetsEachIteration = new int[iterations][data.nTargets];

        // for each iteration, whether it is the result of a schedule or Monte Carlo search, or whether it is an extrema.
        // extrema are not included in averages.
        boolean[] includeIterationInAverages = new boolean[iterations];
        Arrays.fill(includeIterationInAverages, true);

        // TODO don't hardwire timestep below
        ts.timeStep = 60;

        // times at targets from scheduled search
        int[] scheduledTimesAtTargets = new int[data.nTargets];
        Arrays.fill(scheduledTimesAtTargets, UNREACHED);

        // current iteration
        int iteration = 0;

        // FIXME this should be changed to tolerate a zero-width time range
        for (int departureTime = req.toTime - 60, n = 0; departureTime >= fromTime; departureTime -= 60, n++) {
            if (n % 15 == 0) {
                LOG.info("minute {}", n);
            }

            // run the scheduled search
            this.runRaptorScheduled(initialStops, departureTime);
            this.doPropagation(bestNonTransferTimes, scheduledTimesAtTargets, departureTime);

            // pop in the walk only times; we don't want to force people to ride transit instead of
            // walking a block
            for (int i = 0; i < scheduledTimesAtTargets.length; i++) {
                if (nonTransitTimes[i] != UNREACHED && nonTransitTimes[i] + departureTime < scheduledTimesAtTargets[i])
                    scheduledTimesAtTargets[i] = nonTransitTimes[i] + departureTime;
            }

            // run the frequency searches
            if (data.hasFrequencies) {
                for (int i = 0; i < monteCarloDraws + 2; i++) {
                    // make copies for just this search. We need copies because we can't use dynamic
                    // programming/range-raptor with randomized schedules
                    int[] bestTimesCopy = Arrays.copyOf(bestTimes, bestTimes.length);
                    int[] bestNonTransferTimesCopy = Arrays
                            .copyOf(bestNonTransferTimes, bestNonTransferTimes.length);
                    int[] previousPatternsCopy = Arrays
                            .copyOf(previousPatterns, previousPatterns.length);

                    // special cases: calculate the best and the worst cases as well
                    // Note that this (intentionally) does not affect searches where the user has requested
                    // an assumption other than RANDOM, or stops with transfer rules.
                    RaptorWorkerTimetable.BoardingAssumption requestedBoardingAssumption = req.boardingAssumption;

                    if (i == 0 && req.boardingAssumption == RaptorWorkerTimetable.BoardingAssumption.RANDOM) {
                        req.boardingAssumption = RaptorWorkerTimetable.BoardingAssumption.WORST_CASE;
                        // don't include extrema in averages
                        includeIterationInAverages[iteration] = false;
                    }
                    else if (i == 1 && req.boardingAssumption == RaptorWorkerTimetable.BoardingAssumption.RANDOM) {
                        req.boardingAssumption = RaptorWorkerTimetable.BoardingAssumption.BEST_CASE;
                        // don't include extrema in averages
                        includeIterationInAverages[iteration] = false;
                    }
                    else if (requestedBoardingAssumption == RaptorWorkerTimetable.BoardingAssumption.RANDOM)
                        // use a new Monte Carlo draw each time
                        // included in averages by default
                        offsets.randomize();

                    this.runRaptorFrequency(departureTime, bestTimesCopy, bestNonTransferTimesCopy,
                            previousPatternsCopy);

                    req.boardingAssumption = requestedBoardingAssumption;

                    // do propagation
                    int[] frequencyTimesAtTargets = timesAtTargetsEachIteration[iteration++];
                    System.arraycopy(scheduledTimesAtTargets, 0, frequencyTimesAtTargets, 0,
                            scheduledTimesAtTargets.length);
                    // updates timesAtTargetsEachIteration directly because it has a reference into the array.
                    this.doPropagation(bestNonTransferTimesCopy, frequencyTimesAtTargets,
                            departureTime);

                    // convert to elapsed time
                    for (int t = 0; t < frequencyTimesAtTargets.length; t++) {
                        if (frequencyTimesAtTargets[t] != UNREACHED)
                            frequencyTimesAtTargets[t] -= departureTime;
                    }
                }
            } else {
                final int dt = departureTime;
                timesAtTargetsEachIteration[iteration++] = IntStream.of(scheduledTimesAtTargets)
                        .map(i -> i != UNREACHED ? i - dt : i)
                        .toArray();
            }
        }

        // make sure we filled the array, otherwise results are garbage.
        // This implies a bug in OTP, but it has happened in the past when we did
        // not set the number of iterations correctly.
        // iteration should be incremented past end of array by ++ in assignment above
        if (iteration != iterations)
            throw new IllegalStateException("Iterations did not completely fill output array");

        long calcTime = System.currentTimeMillis() - beginCalcTime;
        LOG.info("calc time {}sec", calcTime / 1000.0);
        LOG.info("  propagation {}sec", totalPropagationTime / 1000.0);
        LOG.info("  raptor {}sec", (calcTime - totalPropagationTime) / 1000.0);
        ts.propagation = (int) totalPropagationTime;
        ts.transitSearch = (int) (calcTime - totalPropagationTime);
        //dumpVariableByte(timesAtTargetsEachMinute);
        // we can use min_max here as we've also run it once with best case and worst case board,
        // so the best and worst cases are meaningful.
        propagatedTimesStore.setFromArray(timesAtTargetsEachIteration, includeIterationInAverages,
                PropagatedTimesStore.ConfidenceCalculationMethod.MIN_MAX);
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
            LOG.error("File not found for dumping raptor results", e);
        } catch (IOException e) {
            LOG.error("IOException dumping raptor results", e);
        }
    }

    /** Run a raptor search not using frequencies */
    public void runRaptorScheduled (TIntIntMap initialStops, int departureTime) {
        // Arrays.fill(bestTimes, UNREACHED); hold on to old state
        max_time = departureTime + MAX_DURATION;
        round = 0;
        advance(); // go to first round
        patternsTouched.clear(); // clear patterns left over from previous calls.
        allStopsTouched.clear();
        stopsTouched.clear();
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
        while (doOneRound(bestTimes, bestNonTransferTimes, previousPatterns, false)) {
            advance();
        }
    }

    /** Run a RAPTOR search using frequencies */
    public void runRaptorFrequency (int departureTime, int[] bestTimes, int[] bestNonTransferTimes, int[] previousPatterns) {
        max_time = departureTime + MAX_DURATION;
        round = 0;
        advance(); // go to first round
        patternsTouched.clear(); // clear patterns left over from previous calls.
        allStopsTouched.clear();
        stopsTouched.clear();

        // we need to mark every reachable stop here, because the network is changing randomly.
        // It is entirely possible that the first trip in an itinerary does not change, but trips
        // further down do.
        IntStream.range(0, bestTimes.length).filter(i -> bestTimes[i] != UNREACHED).forEach(
                this::markPatternsForStop);

        // Anytime a round updates some stops, move on to another round
        while (doOneRound(bestTimes, bestNonTransferTimes, previousPatterns, true)) {
            advance();
        }
    }

    public boolean doOneRound (int[] bestTimes, int[] bestNonTransferTimes, int[] previousPatterns, boolean useFrequencies) {
        //LOG.info("round {}", round);
        stopsTouched.clear(); // clear any stops left over from previous round.
        PATTERNS: for (int p = patternsTouched.nextSetBit(0); p >= 0; p = patternsTouched.nextSetBit(p+1)) {
            //LOG.info("pattern {} {}", p, data.patternNames.get(p));
            int onTrip = -1;
            RaptorWorkerTimetable timetable = data.timetablesForPattern.get(p);
            int stopPositionInPattern = -1; // first increment will land this at zero

            int bestFreqBoardTime = Integer.MAX_VALUE;
            int bestFreqBoardStop = -1;
            int bestFreq = -1;

            // first look for a frequency entry
            if (useFrequencies) {
                for (int stopIndex : timetable.stopIndices) {
                    stopPositionInPattern += 1;

                    // the time at this stop if we remain on board a vehicle we had already boarded
                    int remainOnBoardTime;
                    if (bestFreq != -1) {
                        // we are already aboard a trip, stay on board
                        remainOnBoardTime = bestFreqBoardTime + timetable
                                .getFrequencyTravelTime(bestFreq, bestFreqBoardStop,
                                        stopPositionInPattern);
                    } else {
                        // we cannot remain on board as we are not yet on board
                        remainOnBoardTime = Integer.MAX_VALUE;
                    }

                    // the time at this stop if we board a new vehicle
                    if (bestTimes[stopIndex] != UNREACHED) {
                        for (int trip = 0; trip < timetable.getFrequencyTripCount(); trip++) {
                            int boardTime = timetable
                                    .getFrequencyDeparture(trip, stopPositionInPattern,
                                            bestTimes[stopIndex], previousPatterns[stopIndex], offsets, req.boardingAssumption);

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
                            allStopsTouched.set(stopIndex);

                            if (bestTimes[stopIndex] > remainOnBoardTime) {
                                bestTimes[stopIndex] = remainOnBoardTime;
                                previousPatterns[stopIndex] = p;
                            }
                        }
                    }
                }

                // don't mix frequencies and timetables
                if (bestFreq != -1)
                    continue PATTERNS;
            }

            // perform scheduled search
            stopPositionInPattern = -1;

            for (int stopIndex : timetable.stopIndices) {
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
                        allStopsTouched.set(stopIndex);

                        if (arrivalTime < bestTimes[stopIndex]) {
                            bestTimes[stopIndex] = arrivalTime;
                            previousPatterns[stopIndex] = p;
                        }

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
        doTransfers(bestTimes, bestNonTransferTimes, previousPatterns);
        return !patternsTouched.isEmpty();
    }

    /**
     * Apply transfers.
     * Mark all the patterns passing through these stops and any stops transferred to.
     */
    private void doTransfers(int[] bestTimes, int[] bestNonTransferTimes, int[] previousPatterns) {
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
                    previousPatterns[toStop] = previousPatterns[stop];
                    markPatternsForStop(toStop);
                }
            }
        }
    }

    /**
     * Propagate from the transit network to the street network.
     * Uses allStopsTouched to determine from whence to propagate.
     *
     * This is valid both for randomized frequencies and for schedules, because the stops that have
     * been updated will be in allStopsTouched.
     */
    public void doPropagation (int[] timesAtTransitStops, int[] timesAtTargets, int departureTime) {
        long beginPropagationTime = System.currentTimeMillis();

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

        // only loop over stops that were touched this minute
        for (int s = allStopsTouched.nextSetBit(0); s >= 0; s = allStopsTouched.nextSetBit(s + 1)) {
            // it's safe to use the best time at this stop for any number of transfers, even in range-raptor,
            // because we allow unlimited transfers. this is slightly different from the original RAPTOR implementation:
            // we do not necessarily compute all pareto-optimal paths on (journey time, number of transfers).
            int baseTimeSeconds = timesAtTransitStops[s];
            if (baseTimeSeconds != UNREACHED) {
                int[] targets = data.targetsForStop.get(s);

                if (targets == null)
                    continue;

                for (int i = 0; i < targets.length; i++) {
                    int targetIndex = targets[i++]; // increment i after read
                    // the cache has time in seconds rather than distance, to avoid costly floating-point divides and integer casts here.
                    int propagated_time = baseTimeSeconds + targets[i];

                    if (timesAtTargets[targetIndex] > propagated_time) {
                        timesAtTargets[targetIndex] = propagated_time;
                    }
                }
            }
        }
        totalPropagationTime += (System.currentTimeMillis() - beginPropagationTime);
    }

    /** Mark all the patterns passing through the given stop. */
    private void markPatternsForStop(int stop) {
        int[] patterns = data.patternsForStop.get(stop);
        for (int pattern : patterns) {
            patternsTouched.set(pattern);
        }
    }

}
