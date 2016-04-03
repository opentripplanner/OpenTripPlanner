package org.opentripplanner.profile;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.opentripplanner.analyst.cluster.TaskStatistics;
import org.opentripplanner.analyst.scenario.AddTripPattern;
import org.opentripplanner.analyst.scenario.Scenario;
import org.opentripplanner.analyst.scenario.TransferRule;
import org.opentripplanner.analyst.scenario.TripFilter;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A RaptorWorkerTimetable is used by a RaptorWorker to perform large numbers of RAPTOR searches very quickly
 * within a specific time window. It is used heavily in one-to-many profile routing, where we're interested in seeing
 * how access to opportunities varies over time.
 *
 * This is an alternative representation of all the TripTimes in a single TripPattern that are running during a
 * particular time range on a particular day. It packs the times a bit closer together and pre-filters the TripTimes based on
 * whether they are running at all during the time window eliminates run-time checks that need to be performed when we
 * search for the soonest departure. Note: we're not sure this actually provides a major speedup compared to operating
 * directly on TripPatterns and TripTimes.
 *
 * Unlike in "normal" OTP searches, we assume non-overtaking (FIFO) vehicle behavior within a single TripPattern,
 * which is generally the case in clean input data. One key difference here is that profile routing and spatial analysis
 * do not need to take real-time (GTFS-RT) updates into account since they are intended to be generic results
 * describing a scenario in the future.
 */
public class RaptorWorkerTimetable implements Serializable {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorWorkerTimetable.class);

    /* Times for schedule-based trips/patterns are stored in a 2D array. */

    int nTrips, nStops;

    /* For each trip on this pattern, an packed array of (arrival, departure) time pairs. */
    public int[][] timesPerTrip;

    /* Times for frequency-based trips are stored in parallel arrays (a column store). */

    /** Times (0-based) for frequency trips */
    int[][] frequencyTrips;

    /** Headways (seconds) for frequency trips, parallel to above. Note that frequency trips are unsorted. */
    int[] headwaySecs;

    /** Start times (seconds since noon - 12h) for frequency trips */
    int[] startTimes;

    /** End times for frequency trips */
    int[] endTimes;

    /** Indices of stops in parent data */
    public int[] stopIndices;

    /** parent raptorworkerdata of this timetable */
    public RaptorWorkerData raptorData;

    /** Mode of this pattern, see constants in com.conveyal.gtfs.model.Route */
    public int mode;

    /** Index of this pattern in RaptorData */
    public int dataIndex;

    /** for debugging, the ID of the route this represents */
    public transient String routeId;

    /** slack required when boarding a transit vehicle */
    public static final int MIN_BOARD_TIME_SECONDS = 60;

    public RaptorWorkerTimetable(int nTrips, int nStops) {
        this.nTrips = nTrips;
        this.nStops = nStops;
        timesPerTrip = new int[nTrips][];
    }

    /**
     * Return the trip index within the pattern of the soonest departure at the given stop number, requiring at least
     * MIN_BOARD_TIME_SECONDS seconds of slack. 
     */
    public int findDepartureAfter(int stop, int time) {
        for (int trip = 0; trip < timesPerTrip.length; trip++) {
            if (getDeparture(trip, stop) > time + MIN_BOARD_TIME_SECONDS) {
                return trip;
            }
        }
        return -1;
    }

    public int getArrival (int trip, int stop) {
        return timesPerTrip[trip][stop * 2];
    }

    public int getDeparture (int trip, int stop) {
        return timesPerTrip[trip][stop * 2 + 1];
    }

    public int getFrequencyDeparture (int trip, int stop, int time, int previousPattern, FrequencyRandomOffsets offsets) {
        return getFrequencyDeparture(trip, stop, time, previousPattern, offsets, null);
    }

    /**
     * Get the departure on frequency trip trip at stop stop after time time,
     * with the given boarding assumption. (Note that the boarding assumption specified may be overridden
     * by transfer rules).
     */
    public int getFrequencyDeparture (int trip, int stop, int time, int previousPattern, FrequencyRandomOffsets offsets, BoardingAssumption assumption) {
        int timeToReachStop = frequencyTrips[trip][stop * 2 + 1];

        // figure out if there is an applicable transfer rule
        TransferRule transferRule = null;

        if (previousPattern != -1) {
            // this is a transfer

            // stop index in Raptor data
            int stopIndex = stopIndices[stop];

            // first check for specific rules
            // calling containsKey can be expensive so first check if list is empty
            if (!raptorData.transferRules.isEmpty() && raptorData.transferRules.containsKey(stopIndex)) {
                for (TransferRule tr : raptorData.transferRules.get(stopIndex)) {
                    if (tr.matches(raptorData.timetablesForPattern.get(previousPattern), this)) {
                        transferRule = tr;
                        break;
                    }
                }
            }

            if (transferRule == null && !raptorData.baseTransferRules.isEmpty()) {
                // look for global rules
                // using declarative for loop because constructing a stream and doing a filter is
                // slow.
                for (TransferRule tr : raptorData.baseTransferRules) {
                    if (tr.matches(raptorData.timetablesForPattern.get(previousPattern), this)) {
                        transferRule = tr;
                        break;
                    }
                }
            }
        }

        if (assumption == null)
            assumption = raptorData.boardingAssumption;

        // if there is an applicable transfer rule, override anything that was specified elsewhere
        if (transferRule != null)
            assumption = transferRule.assumption;

        if (assumption == BoardingAssumption.RANDOM) {
            // We treat every frequency-based trip as a scheduled trip on each iteration of the Monte Carlo
            // algorithm. The reason for this is thus: consider something like the Portland Transit Mall.
            // There are many opportunities to transfer between vehicles. The transfer times between
            // Line A and Line B are not independently random at each stop but rather are correlated
            // along each line. Thus we randomize each pattern independently, not each boarding.

            // Keep in mind that there could also be correlation between patterns, especially for rail
            // vehicles that share trackage. Consider, for example, the Fredericksburg and Manassus
            // lines on the Virginia Railway Express, at Alexandria. These are two diesel heavy-rail
            // lines that share trackage. Thus the minimum transfer time between them is always at least
            // 5 minutes or so as that's as close as you can run diesel trains to each other.
            int minTime = startTimes[trip] + timeToReachStop + offsets.offsets.get(this.dataIndex)[trip];

            // move time forward to an integer multiple of headway after the minTime
            if (time < minTime)
                time = minTime;
            else
                // add enough to time to make time - minTime an integer multiple of headway
                // if the bus comes every 30 minutes and you arrive at the stop 35 minutes
                // after it first came, you have to wait 25 minutes, or headway - time already elapsed
                // time already elapsed is 35 % 30 = 5 minutes in this case.
                time += headwaySecs[trip] - (time - minTime) % headwaySecs[trip];
        }

        else {
            // move time forward if the frequency has not yet started.
            // we do this before we add the wait time, because this is the earliest possible
            // time a vehicle could reach the stop. The assumption is that the vehicle leaves
            // the terminal between zero and headway_seconds seconds after the start_time of the
            // frequency
            if (timeToReachStop + startTimes[trip] > time)
                time = timeToReachStop + startTimes[trip];

            switch (assumption) {
            case BEST_CASE:
                // do nothing
                break;
            case WORST_CASE:
                time += headwaySecs[trip];
                break;
            case HALF_HEADWAY:
                time += headwaySecs[trip] / 2;
                break;
            case FIXED:
                if (transferRule == null) throw new IllegalArgumentException("Cannot use boarding assumption FIXED without a transfer rule");
                time += transferRule.transferTimeSeconds;
                break;
            case PROPORTION:
                if (transferRule == null) throw new IllegalArgumentException("Cannot use boarding assumption PROPORTION without a transfer rule");
                time += (int) (transferRule.waitProportion * headwaySecs[trip]);
                break;
            }
        }

        if (time > timeToReachStop + endTimes[trip])
            return -1;
        else
            return time;
    }

    /**
     * Get the travel time (departure to arrival) on frequency trip trip, from stop from to stop to.  
     */
    public int getFrequencyTravelTime (int trip, int from, int to) {
        return frequencyTrips[trip][to * 2] - frequencyTrips[trip][from * 2 + 1];
    }

    /**
     * Get the number of frequency trips on this pattern (i.e. the number of trips in trips.txt, not the number of trips by vehicles)
     */
    public int getFrequencyTripCount () {
        return headwaySecs.length;
    }

    /** Does this timetable have any frequency trips? */
    public boolean hasFrequencyTrips () {
        return this.headwaySecs != null && this.headwaySecs.length > 0;
    }

    /** does this timetable have any scheduled trips? */
    public boolean hasScheduledTrips () {
        return this.timesPerTrip != null && this.timesPerTrip.length > 0;
    }

    /**
     * This is a factory function rather than a constructor to avoid calling the super constructor for rejected patterns.
     * BannedRoutes is formatted as agencyid_routeid.
     */
    public static RaptorWorkerTimetable forPattern (Graph graph, TripPattern pattern, TimeWindow window, Scenario scenario, TaskStatistics ts) {

        // Filter down the trips to only those running during the window
        // This filtering can reduce number of trips and run time by 80 percent
        BitSet servicesRunning = window.servicesRunning;
        List<TripTimes> tripTimes = Lists.newArrayList();
        TT: for (TripTimes tt : pattern.scheduledTimetable.tripTimes) {
            if (servicesRunning.get(tt.serviceCode) &&
                    tt.getArrivalTime(0) < window.to &&
                    tt.getDepartureTime(tt.getNumStops() - 1) >= window.from) {

                // apply scenario
                // TODO: need to do this before filtering based on window!
                if (scenario != null && scenario.modifications != null) {
                    for (TripFilter filter : Iterables.filter(scenario.modifications, TripFilter.class)) {
                        tt = filter.apply(tt.trip, pattern, tt);

                        if (tt == null)
                            continue TT;
                    }
                }

                tripTimes.add(tt);
            }
        }

        // find frequency trips
        List<FrequencyEntry> freqs = Lists.newArrayList();
        FREQUENCIES: for (FrequencyEntry fe : pattern.scheduledTimetable.frequencyEntries) {
            if (servicesRunning.get(fe.tripTimes.serviceCode) &&
                    fe.getMinDeparture() < window.to &&
                    fe.getMaxArrival() > window.from
                    ) {
                // this frequency entry has the potential to be used

                if (fe.exactTimes) {
                    LOG.warn("Exact-times frequency trips not yet supported");
                    continue;
                }

                if (scenario != null && scenario.modifications != null) {
                    for (TripFilter filter : Iterables.filter(scenario.modifications, TripFilter.class)) {
                        fe = filter.apply(fe.tripTimes.trip, pattern, fe);

                        if (fe == null)
                            continue FREQUENCIES;
                    }
                }

                freqs.add(fe);
            }
        }

        if (tripTimes.isEmpty() && freqs.isEmpty()) {
            return null; // no trips active, don't bother storing a timetable
        }


        // Sort the trip times by their first arrival time
        Collections.sort(tripTimes, new Comparator<TripTimes>() {
            @Override
            public int compare(TripTimes tt1, TripTimes tt2) {
                return (tt1.getArrivalTime(0) - tt2.getArrivalTime(0));
            }
        });

        // Copy the times into the compacted table
        RaptorWorkerTimetable rwtt = new RaptorWorkerTimetable(tripTimes.size(), pattern.getStops().size());
        int t = 0;
        for (TripTimes tt : tripTimes) {
            int[] times = new int[rwtt.nStops * 2];
            for (int s = 0; s < pattern.getStops().size(); s++) {
                int arrival = tt.getArrivalTime(s);
                int departure = tt.getDepartureTime(s);
                times[s * 2] = arrival;
                times[s * 2 + 1] = departure;
            }
            rwtt.timesPerTrip[t++] = times;
        }

        ts.scheduledTripCount += rwtt.timesPerTrip.length;

        // save frequency times
        rwtt.frequencyTrips = new int[freqs.size()][pattern.getStops().size() * 2];
        rwtt.endTimes = new int[freqs.size()];
        rwtt.startTimes = new int[freqs.size()];
        rwtt.headwaySecs = new int[freqs.size()];

        {
            int i = 0;
            for (FrequencyEntry fe : freqs) {
                rwtt.headwaySecs[i] = fe.headway;
                rwtt.startTimes[i] = fe.startTime;
                rwtt.endTimes[i] = fe.endTime;

                ts.frequencyTripCount += fe.numTrips();

                int[] times = rwtt.frequencyTrips[i];

                // It's generally considered good practice to have frequency trips start at midnight, however that is
                // not always the case, and we need to preserve the original times so that we can update them in
                // real time.
                int startTime = fe.tripTimes.getArrivalTime(0);

                for (int s = 0; s < fe.tripTimes.getNumStops(); s++) {
                    times[s * 2] = fe.tripTimes.getArrivalTime(s) - startTime;
                    times[s * 2 + 1] = fe.tripTimes.getDepartureTime(s) - startTime;
                }

                i++;
            }
        }

        ts.frequencyEntryCount += rwtt.getFrequencyTripCount();

        rwtt.mode = pattern.route.getType();

        return rwtt;
    }

    /** Create a raptor worker timetable for an added pattern */
    public static RaptorWorkerTimetable forAddedPattern(AddTripPattern atp, TimeWindow window, TaskStatistics ts) {
        if (atp.temporaryStops.length < 2 || atp.timetables.isEmpty())
            return null;

        // filter down the timetable to only those running
        Collection<AddTripPattern.PatternTimetable> running = atp.timetables.stream()
                // getValue yields one-based ISO days of week (monday = 1); convert to 0-based format
                .filter(t -> t.days.get(window.dayOfWeek.getValue() - 1))
                .collect(Collectors.toList());

        if (running.isEmpty())
            return null;

        // TODO: filter with timewindow
        Collection<AddTripPattern.PatternTimetable> frequencies = running.stream()
                .filter(t -> t.frequency)
                .collect(Collectors.toList());

        Collection<AddTripPattern.PatternTimetable> timetables = running.stream()
                .filter(t -> !t.frequency)
                .sorted((t1, t2) -> t1.startTime - t2.startTime)
                .collect(Collectors.toList());

        RaptorWorkerTimetable rwtt = new RaptorWorkerTimetable(timetables.size(), atp.temporaryStops.length);

        // create timetabled trips
        int t = 0;
        for (AddTripPattern.PatternTimetable pt : timetables) {
            rwtt.timesPerTrip[t++] = timesForPatternTimetable(atp, pt);
        }

        ts.scheduledTripCount += rwtt.timesPerTrip.length;

        // create frequency trips
        rwtt.frequencyTrips = new int[frequencies.size()][atp.temporaryStops.length * 2];
        rwtt.endTimes = new int[frequencies.size()];
        rwtt.startTimes = new int[frequencies.size()];
        rwtt.headwaySecs = new int[frequencies.size()];

        t = 0;
        for (AddTripPattern.PatternTimetable pt : frequencies) {
            rwtt.frequencyTrips[t] = timesForPatternTimetable(atp, pt);
            rwtt.startTimes[t] = pt.startTime;
            rwtt.endTimes[t] = pt.endTime;
            rwtt.headwaySecs[t++] = pt.headwaySecs;

            ts.frequencyTripCount += (pt.endTime - pt.startTime) / pt.headwaySecs;
        }

        ts.frequencyEntryCount += frequencies.size();

        rwtt.mode = atp.mode;
        rwtt.routeId = atp.name;

        return rwtt;
    }

    private static int[] timesForPatternTimetable (AddTripPattern atp, AddTripPattern.PatternTimetable pt) {
        int[] times = new int[atp.temporaryStops.length * 2];
        for (int s = 0; s < atp.temporaryStops.length; s++) {
            // for a timetable route,
            // arrival time is start time if it's the first stop, or the previous departure time plus the hop time
            // For a frequency route the first departure is always 0
            if (s == 0)
                times[s * 2] = pt.frequency ? 0 : pt.startTime;
            else
                times[s * 2] = times[s * 2 - 1] + pt.hopTimes[s - 1];

            times[s * 2 + 1] = times[s * 2] + pt.dwellTimes[s];
        }
        return times;
    }

    /** The assumptions made when boarding a frequency vehicle: best case (no wait), worst case (full headway) and half headway (in some sense the average). */
    public static enum BoardingAssumption {
        BEST_CASE, WORST_CASE, HALF_HEADWAY, FIXED, PROPORTION, RANDOM;

        public static final long serialVersionUID = 1;
    }

}
