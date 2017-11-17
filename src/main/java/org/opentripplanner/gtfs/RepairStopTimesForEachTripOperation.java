package org.opentripplanner.gtfs;

import gnu.trove.list.TIntList;
import gnu.trove.list.array.TIntArrayList;
import org.opentripplanner.common.geometry.SphericalDistanceLibrary;
import org.opentripplanner.graph_builder.annotation.HopSpeedFast;
import org.opentripplanner.graph_builder.annotation.HopSpeedSlow;
import org.opentripplanner.graph_builder.annotation.HopZeroTime;
import org.opentripplanner.graph_builder.annotation.NegativeDwellTime;
import org.opentripplanner.graph_builder.annotation.NegativeHopTime;
import org.opentripplanner.graph_builder.annotation.RepeatedStops;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripStopTimes;
import org.opentripplanner.routing.edgetype.factory.PatternHopFactory;
import org.opentripplanner.routing.graph.AddBuilderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is responsible for cleaning stop times, removing duplicates, correcting bad data
 * and so on. This was previously done in the
 * {@link PatternHopFactory}, and the code is
 * extracted out of it to make the PatternHopFactory reusable for NETEX and GTFS file import.
 */
public class RepairStopTimesForEachTripOperation {
    private static final Logger LOG = LoggerFactory.getLogger(RepairStopTimesForEachTripOperation.class);

    private static final int SECONDS_IN_HOUR = 60 * 60;

    private final TripStopTimes stopTimesByTrip;

    private AddBuilderAnnotation builderAnnotation;

    public RepairStopTimesForEachTripOperation(TripStopTimes stopTimesByTrip, AddBuilderAnnotation builderAnnotation) {
        this.stopTimesByTrip = stopTimesByTrip;
        this.builderAnnotation = builderAnnotation;
    }

    public void run() {
        final int tripSize = stopTimesByTrip.size();
        int tripCount = 0;

        for (Trip trip : stopTimesByTrip.keys()) {
            if (++tripCount % 100000 == 0) {
                LOG.debug("Repair StopTimes for trips {}/{}", tripCount, tripSize);
            }

            /* Fetch the stop times for this trip. Copy the list since it's immutable. */
            List<StopTime> stopTimes = new ArrayList<>(stopTimesByTrip.get(trip));

            /* Stop times frequently contain duplicate, missing, or incorrect entries. Repair them. */
            TIntList removedStopSequences = removeRepeatedStops(stopTimes);
            if (!removedStopSequences.isEmpty()) {
                LOG.warn(builderAnnotation
                        .addBuilderAnnotation(new RepeatedStops(trip, removedStopSequences)));
            }
            filterStopTimes(stopTimes);
            interpolateStopTimes(stopTimes);

            stopTimesByTrip.replace(trip, stopTimes);
        }
    }

    /**
     * Filter out any series of stop times that refer to the same stop. This is very inefficient in
     * an array-backed list, but we are assuming that this is a rare occurrence. The alternative is
     * to copy every list of stop times during filtering.
     *
     * TODO: OBA GFTS makes the stoptime lists unmodifiable, so this will not work.
     * We need to copy any modified list.
     *
     * @return whether any repeated stops were filtered out.
     */
    private TIntList removeRepeatedStops(List<StopTime> stopTimes) {
        StopTime prev = null;
        Iterator<StopTime> it = stopTimes.iterator();
        TIntList stopSequencesRemoved = new TIntArrayList();
        while (it.hasNext()) {
            StopTime st = it.next();
            if (prev != null) {
                if (prev.getStop().equals(st.getStop())) {
                    // OBA gives us unmodifiable lists, but we have copied them.

                    // Merge the two stop times, making sure we're not throwing out a stop time with times in favor of an
                    // interpolated stop time
                    // keep the arrival time of the previous stop, unless it didn't have an arrival time, in which case
                    // replace it with the arrival time of this stop time
                    // This is particularly important at the last stop in a route (see issue #2220)
                    if (prev.getArrivalTime() == StopTime.MISSING_VALUE)
                        prev.setArrivalTime(st.getArrivalTime());

                    // prefer to replace with the departure time of this stop time, unless this stop time has no departure time
                    if (st.getDepartureTime() != StopTime.MISSING_VALUE)
                        prev.setDepartureTime(st.getDepartureTime());

                    it.remove();
                    stopSequencesRemoved.add(st.getStopSequence());
                }
            }
            prev = st;
        }
        return stopSequencesRemoved;
    }

    /**
     * Scan through the given list, looking for clearly incorrect series of stoptimes and unsetting
     * them. This includes duplicate times (0-time hops), as well as negative, fast or slow hops.
     * Unsetting the arrival/departure time of clearly incorrect stoptimes will cause them to be
     * interpolated in the next step. Annotations are also added to the graph to reveal the problems
     * to the user.
     *
     * @param stopTimes the stoptimes to be filtered (from a single trip)
     */
    private void filterStopTimes(List<StopTime> stopTimes) {

        if (stopTimes.size() < 2)
            return;
        StopTime st0 = stopTimes.get(0);

        /* Set departure time if it is missing */
        if (!st0.isDepartureTimeSet() && st0.isArrivalTimeSet()) {
            st0.setDepartureTime(st0.getArrivalTime());
        }

        /* If the feed does not specify any timepoints, we want to mark all times that are present as timepoints. */
        boolean hasTimepoints = false;
        for (StopTime stopTime : stopTimes) {
            if (stopTime.getTimepoint() == 1) {
                hasTimepoints = true;
                break;
            }
        }
        // TODO verify that the first (and last?) stop should always be considered a timepoint.
        if (!hasTimepoints)
            st0.setTimepoint(1);

        /* Indicates that stop times in this trip are being shifted forward one day. */
        boolean midnightCrossed = false;

        for (int i = 1; i < stopTimes.size(); i++) {
            boolean st1bogus = false;
            StopTime st1 = stopTimes.get(i);

            /* If the feed did not specify any timepoints, mark all times that are present as timepoints. */
            if (!hasTimepoints && (st1.isDepartureTimeSet() || st1.isArrivalTimeSet())) {
                st1.setTimepoint(1);
            }

            if (midnightCrossed) {
                if (st1.isDepartureTimeSet())
                    st1.setDepartureTime(st1.getDepartureTime() + 24 * SECONDS_IN_HOUR);
                if (st1.isArrivalTimeSet())
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * SECONDS_IN_HOUR);
            }
            /* Set departure time if it is missing. */
            // TODO: doc: what if arrival time is missing?
            if (!st1.isDepartureTimeSet() && st1.isArrivalTimeSet()) {
                st1.setDepartureTime(st1.getArrivalTime());
            }
            /* Do not process (skip over) non-timepoint stoptimes, leaving them in place for interpolation. */
            // All non-timepoint stoptimes in a series will have identical arrival and departure values of MISSING_VALUE.
            if (!(st1.isArrivalTimeSet() && st1.isDepartureTimeSet())) {
                continue;
            }
            int dwellTime = st0.getDepartureTime() - st0.getArrivalTime();
            if (dwellTime < 0) {
                LOG.warn(builderAnnotation.addBuilderAnnotation(new NegativeDwellTime(st0)));
                if (st0.getArrivalTime() > 23 * SECONDS_IN_HOUR
                        && st0.getDepartureTime() < 1 * SECONDS_IN_HOUR) {
                    midnightCrossed = true;
                    st0.setDepartureTime(st0.getDepartureTime() + 24 * SECONDS_IN_HOUR);
                } else {
                    st0.setDepartureTime(st0.getArrivalTime());
                }
            }
            int runningTime = st1.getArrivalTime() - st0.getDepartureTime();

            if (runningTime < 0) {
                LOG.warn(builderAnnotation.addBuilderAnnotation(
                        new NegativeHopTime(new StopTime(st0), new StopTime(st1))));
                // negative hops are usually caused by incorrect coding of midnight crossings
                midnightCrossed = true;
                if (st0.getDepartureTime() > 23 * SECONDS_IN_HOUR
                        && st1.getArrivalTime() < 1 * SECONDS_IN_HOUR) {
                    st1.setArrivalTime(st1.getArrivalTime() + 24 * SECONDS_IN_HOUR);
                } else {
                    st1.setArrivalTime(st0.getDepartureTime());
                }
            }
            double hopDistance = SphericalDistanceLibrary
                    .fastDistance(st0.getStop().getLat(), st0.getStop().getLon(),
                            st1.getStop().getLat(), st1.getStop().getLon());
            double hopSpeed = hopDistance / runningTime;
            /* zero-distance hops are probably not harmful, though they could be better
             * represented as dwell times
            if (hopDistance == 0) {
                LOG.warn(GraphBuilderAnnotation.register(graph,
                        Variety.HOP_ZERO_DISTANCE, runningTime,
                        st1.getTrip().getId(),
                        st1.getStopSequence()));
            }
            */
            // sanity-check the hop
            if (st0.getArrivalTime() == st1.getArrivalTime() || st0.getDepartureTime() == st1
                    .getDepartureTime()) {
                LOG.trace("{} {}", st0, st1);
                // series of identical stop times at different stops
                LOG.trace(builderAnnotation.addBuilderAnnotation(
                        new HopZeroTime((float) hopDistance, st1.getTrip(),
                                st1.getStopSequence())));
                // clear stoptimes that are obviously wrong, causing them to later be interpolated
/* FIXME (lines commented out because they break routability in multi-feed NYC for some reason -AMB) */
                //                st1.clearArrivalTime();
                //                st1.clearDepartureTime();
                st1bogus = true;
            } else if (hopSpeed > 45) {
                // 45 m/sec ~= 100 miles/hr
                // elapsed time of 0 will give speed of +inf
                LOG.trace(builderAnnotation.addBuilderAnnotation(
                        new HopSpeedFast((float) hopSpeed, (float) hopDistance, st0.getTrip(),
                                st0.getStopSequence())));
            } else if (hopSpeed < 0.1) {
                // 0.1 m/sec ~= 0.2 miles/hr
                LOG.trace(builderAnnotation.addBuilderAnnotation(
                        new HopSpeedSlow((float) hopSpeed, (float) hopDistance, st0.getTrip(),
                                st0.getStopSequence())));
            }
            // st0 should reflect the last stoptime that was not clearly incorrect
            if (!st1bogus)
                st0 = st1;
        } // END for loop over stop times
    }

    /**
     * Scan through the given list of stoptimes, interpolating the missing (unset) ones.
     * This is currently done by assuming equidistant stops and constant speed.
     * While we may not be able to improve the constant speed assumption, we can
     * TODO: use route matching (or shape distance etc.) to improve inter-stop distances
     *
     * @param stopTimes the stoptimes (from a single trip) to be interpolated
     */
    private void interpolateStopTimes(List<StopTime> stopTimes) {
        int lastStop = stopTimes.size() - 1;
        int numInterpStops = -1;
        int departureTime = -1, prevDepartureTime = -1;
        int interpStep = 0;

        int i;
        for (i = 0; i < lastStop; i++) {
            StopTime st0 = stopTimes.get(i);

            prevDepartureTime = departureTime;
            departureTime = st0.getDepartureTime();

            /* Interpolate, if necessary, the times of non-timepoint stops */
            /* genuine interpolation needed */
            if (!(st0.isDepartureTimeSet() && st0.isArrivalTimeSet())) {
                // figure out how many such stops there are in a row.
                int j;
                StopTime st = null;
                for (j = i + 1; j < lastStop + 1; ++j) {
                    st = stopTimes.get(j);
                    if ((st.isDepartureTimeSet() && st.getDepartureTime() != departureTime) || (
                            st.isArrivalTimeSet() && st.getArrivalTime() != departureTime)) {
                        break;
                    }
                }
                if (j == lastStop + 1) {
                    throw new RuntimeException(
                            "Could not interpolate arrival/departure time on stop " + i
                                    + " (missing final stop time) on trip " + st0.getTrip());
                }
                numInterpStops = j - i;
                int arrivalTime;
                if (st.isArrivalTimeSet()) {
                    arrivalTime = st.getArrivalTime();
                } else {
                    arrivalTime = st.getDepartureTime();
                }
                interpStep = (arrivalTime - prevDepartureTime) / (numInterpStops + 1);
                if (interpStep < 0) {
                    throw new RuntimeException("trip goes backwards for some reason");
                }
                for (j = i; j < i + numInterpStops; ++j) {
                    //System.out.println("interpolating " + j + " between " + prevDepartureTime + " and " + arrivalTime);
                    departureTime = prevDepartureTime + interpStep * (j - i + 1);
                    st = stopTimes.get(j);
                    if (st.isArrivalTimeSet()) {
                        departureTime = st.getArrivalTime();
                    } else {
                        st.setArrivalTime(departureTime);
                    }
                    if (!st.isDepartureTimeSet()) {
                        st.setDepartureTime(departureTime);
                    }
                }
                i = j - 1;
            }
        }
    }

}
