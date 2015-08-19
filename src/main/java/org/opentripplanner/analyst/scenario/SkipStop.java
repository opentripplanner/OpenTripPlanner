package org.opentripplanner.analyst.scenario;

import com.google.common.collect.Lists;
import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.trippattern.Deduplicator;
import org.opentripplanner.routing.trippattern.FrequencyEntry;
import org.opentripplanner.routing.trippattern.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Skip stops and associated dwell times.
 *
 * Skipped stops are no longer served by the matched trips, and and dwell time at a skipped stop is removed from the schedule.
 * If stops are skipped at the start of a trip, the start of the trip is simply removed; the remaining times are not shifted.
 */
public class SkipStop extends TripPatternFilter {
    public static final long serialVersionUID = 1L;

    private static final Logger LOG = LoggerFactory.getLogger(SkipStop.class);

    /** Stops to skip. Note that setting this to null as a wildcard is not supported, obviously */
    public Collection<String> stopId;

    @Override
    public String getType() {
        return "skip-stop";
    }

    @Override
    public Collection<TripPattern> apply(TripPattern original) {
        if (!couldMatch(original))
            return Arrays.asList(original);

        // figure out which stops we skip
        TIntList skippedStops = new TIntArrayList();
        // retained stops
        // we use stop times to carry a little additional information, e.g. pickup/dropoff type.
        List<StopTime> stopTimes = Lists.newArrayList();

        {
            int i = 0;
            for (Stop stop : original.getStops()) {
                if (stopId.contains(stop.getId().getId()))
                    skippedStops.add(i);
                else {
                    // make a fake stop time
                    StopTime stopTime = new StopTime();
                    stopTime.setStop(stop);
                    stopTime.setPickupType(original.stopPattern.pickups[i]);
                    stopTime.setDropOffType(original.stopPattern.dropoffs[i]);
                    stopTimes.add(stopTime);
                }

                i++;
            }
        }

        if (skippedStops.isEmpty()) {
            LOG.warn("No stops found to skip on matched trip pattern {}", original);
            return Arrays.asList(original);
        }

        if (original.getStops().size() - skippedStops.size() < 2) {
            // TODO best way to handle this case?
            LOG.warn("Trip with skipped stops would have less that two stops for TripPattern {}, not skipping stops", original);
            return Arrays.asList(original);
        }

        // make the new stop pattern
        StopPattern sp = new StopPattern(stopTimes);
        TripPattern modified = new TripPattern(original.route, sp);

        // Any trips that are not matched keep the original trip pattern, so put them here.
        TripPattern originalClone = new TripPattern(original.route, original.stopPattern);

        // keep track of what we have to return
        boolean anyTripsMatched = false;
        boolean allTripsMatched = true;

        for (TripTimes tt : original.scheduledTimetable.tripTimes) {
            if (!matches(tt.trip)) {
                // this trip should not be modified
                allTripsMatched = false;
                originalClone.scheduledTimetable.addTripTimes(tt);
            }
            else {
                // This trip should be modified
                anyTripsMatched = true;
                modified.scheduledTimetable.addTripTimes(omitStops(tt, skippedStops.toArray()));
            }
        }

        for (FrequencyEntry fe : original.scheduledTimetable.frequencyEntries) {
            if (!matches(fe.tripTimes.trip)) {
                allTripsMatched = false;
                originalClone.scheduledTimetable.addFrequencyEntry(fe);
            }
            else {
                anyTripsMatched = true;
                TripTimes newtt = omitStops(fe.tripTimes, skippedStops.toArray());
                FrequencyEntry newfe = new FrequencyEntry(fe.startTime, fe.endTime, fe.headway, fe.exactTimes, newtt);
                modified.scheduledTimetable.addFrequencyEntry(newfe);
            }
        }

        if (!anyTripsMatched)
            return Arrays.asList(original);

        List<TripPattern> ret = Lists.newArrayList();

        ret.add(modified);

        if (!allTripsMatched)
            ret.add(originalClone);

        return ret;
    }

    public TripTimes omitStops (TripTimes tt, int... stopsToSkip) {
        TIntSet skipped = new TIntHashSet(stopsToSkip);

        List<StopTime> newSts = Lists.newArrayList();

        int cumulativeTime = -1;
        for (int i = 0; i < tt.getNumStops(); i++) {
            int hopTime = i != 0 ? tt.getArrivalTime(i) - tt.getDepartureTime(i - 1) : 0;
            int dwellTime = tt.getDepartureTime(i) - tt.getArrivalTime(i);

            // handle the first stop(s) being skipped
            if (cumulativeTime != -1)
                // note that we include hopTime before the check if the stop is included but dwell time after,
                // the assumption being that there is no dwell at a skipped stop.
                cumulativeTime += hopTime;

            if (skipped.contains(i))
                continue;

            // this stop is still part of the trip
            // if this stop is now the first stop, get the time
            if (cumulativeTime == -1)
                cumulativeTime = tt.getArrivalTime(i);

            StopTime stopTime = new StopTime();
            stopTime.setArrivalTime(cumulativeTime);
            cumulativeTime += dwellTime;
            stopTime.setDepartureTime(cumulativeTime);

            stopTime.setStopSequence(tt.getStopSequence(i));
            stopTime.setTimepoint(tt.isTimepoint(i) ? 1 : 0);
            newSts.add(stopTime);
        }

        TripTimes newtt = new TripTimes(tt.trip, newSts, new Deduplicator());
        newtt.serviceCode = tt.serviceCode;
        return newtt;
    }
}
