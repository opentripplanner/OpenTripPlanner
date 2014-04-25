/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.routing.trippattern;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Getter;

import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.opentripplanner.common.MavenVersion;
import org.opentripplanner.gtfs.BikeAccess;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StopTransfer;
import org.opentripplanner.routing.core.TransferTable;
import org.opentripplanner.routing.edgetype.TimedTransferEdge;
import org.opentripplanner.routing.request.BannedStopSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TripTimes represents the arrival and departure times for a single trip in an Timetable. It
 * is carried along by States when routing to ensure that they have a consistent, fast view of the
 * trip when realtime updates are being applied.
 * All times are expressed as seconds since midnight (as in GTFS). The indexes into a StopTimes are
 * not stop indexes, but inter-stop segment ("hop") indexes, so hop 0 refers to the hop between
 * stops 0 and 1, and arrival 0 is actually an arrival at stop 1. The main reason for this is that
 * it saves two extra array elements in every stopTimes. It might be worth it to just use stop
 * indexes everywhere for simplicity.
 */
public class TripTimes implements Serializable, Comparable<TripTimes> {

    private static final long serialVersionUID = MavenVersion.VERSION.getUID();
    private static final Logger LOG = LoggerFactory.getLogger(TripTimes.class);

    /**
     * This constant is used for indicating passed stops, fully canceled trips and trips that are
     * otherwise unavailable during routing. It should only be used in a contiguous block at the
     * beginning of the trip and may or may not cover the entire trip. Partially canceling a trip in
     * this way is specifically not allowed.
     */
    public static final int UNAVAILABLE = -1;

    /** The trips whose arrivals and departures are represented by this TripTimes */
    @Getter private final Trip trip;

    /** The code for the service on which this trip runs. For departure search optimizations. */ 
    public int serviceCode = -1;
    
    /**
     * Both trip_headsign and stop_headsign (per stop on a particular trip) are optional GTFS
     * fields. If the headsigns array is null, we will report the trip_headsign (which may also
     * be null) at every stop on the trip. If all the stop_headsigns are the same as the
     * trip_headsign we may also set the headsigns array to null to save space.
     */
    private final String[] headsigns;

    /**
     * The time in seconds after midnight at which the vehicle should begin traversing each
     * inter-stop segment ("hop") according to the original schedule. Field is non-final to support
     * compaction.
     */
    private int[] scheduledDepartureTimes;

    /**
     * The time in seconds after midnight at which the vehicle should arrive at the end of each
     * inter-stop segment ("hop") according to the original schedule. A null value indicates that
     * all dwells are 0-length, and arrival times are to be derived from the departure times array.
     * Field is non-final to support compaction.
     */
    private int[] scheduledArrivalTimes;

    /**
     * The time in seconds after midnight at which the vehicle begins traversing each inter-stop
     * segment ("hop"). A null value indicates that the original schedule still applies. Field is
     * non-final to support compaction.
     */
    private int[] departureTimes;

    /**
     * The time in seconds after midnight at which the vehicle arrives at the end of each
     * inter-stop segment ("hop"). A null value indicates that arrival times are to be derived from
     * the departure times array or the original schedule. Field is non-final to support compaction.
     */
    private int[] arrivalTimes;

    /**
     * These are the GTFS stop sequence numbers, which show the order in which the vehicle visits
     * the stops. Despite the face that the StopPattern or TripPattern enclosing this TripTimes
     * provides an ordered list of Stops, the original stop sequence numbers may still be needed for
     * matching with GTFS-RT update messages. Unfortunately, each individual trip can have totally
     * different sequence numbers for the same stops, so we need to store them at the individual
     * trip level. An effort is made to re-use the sequence number arrays when they are the same
     * across different trips in the same pattern.
     */
    private int[] stopSequences;

    /**
     * Is this trip cancelled?
     */
    @Getter
    private boolean cancelled = false;

    /**
     * The provided stopTimes are assumed to be pre-filtered, valid, and monotonically increasing.
     */
    public TripTimes(Trip trip, List<StopTime> stopTimes) {
        this.trip = trip;
        int nStops = stopTimes.size();
        int nHops = nStops - 1;
        scheduledDepartureTimes = new int[nHops];
        scheduledArrivalTimes = new int[nHops];
        stopSequences = new int[nStops];
        // this might be clearer if time array indexes were stops instead of hops
        for (int hop = 0; hop < nHops; hop++) {
            scheduledDepartureTimes[hop] = stopTimes.get(hop).getDepartureTime();
            scheduledArrivalTimes[hop] = stopTimes.get(hop + 1).getArrivalTime();
        }
        for (int stop = 0; stop < nStops; stop++) {
            StopTime stopTime = stopTimes.get(stop);
            stopSequences[stop] = stopTime.getStopSequence();
        }
        this.headsigns = makeHeadsignsArray(stopTimes);
        // If all dwell times are 0, arrival times array is not needed. Attempt to save some memory.
        this.compact();
    }

    /** This copy constructor does not copy the actual times, only the scheduled times. */
    // TODO might it be more maintainable to clone the triptimes then null out the scheduled times?
    public TripTimes(TripTimes object) {
        this.trip = object.trip;
        this.headsigns = object.headsigns;
        this.scheduledDepartureTimes = object.scheduledDepartureTimes;
        this.scheduledArrivalTimes = object.scheduledArrivalTimes;
        this.stopSequences = object.stopSequences;
    }

    /**
     * @return either an array of headsigns (one for each stop on this trip) or null if the
     * headsign is the same at all stops (including null) and can be found in the Trip object.
     */
    private String[] makeHeadsignsArray(List<StopTime> stopTimes) {
        String tripHeadsign = trip.getTripHeadsign();
        boolean useStopHeadsigns = false;
        if (tripHeadsign == null) {
            useStopHeadsigns = true;
        } else {
            for (StopTime st : stopTimes) {
                if ( ! (tripHeadsign.equals(st.getStopHeadsign()))) {
                    useStopHeadsigns = true;
                    break;
                }
            }
        }
        if (!useStopHeadsigns) {
            return null; //defer to trip_headsign
        }
        boolean allNull = true;
        int i = 0;
        String[] hs = new String[stopTimes.size()];
        for (StopTime st : stopTimes) {
            String headsign = st.getStopHeadsign();
            hs[i++] = headsign;
            if (headsign != null) allNull = false;
        }
        if (allNull) {
            return null;
        } else {
            return hs;
        }
    }

    /** @return the number of inter-stop segments (hops) on this trip */
    public int getNumHops() {
        // The arrivals array may not be present, and the departures array may have grown by 1 due
        // to compaction, so we can't directly use array lengths as an indicator of number of hops.
        if (scheduledArrivalTimes == null) {
            return scheduledDepartureTimes.length - 1;
        } else {
            return scheduledArrivalTimes.length;
        }
    }

    /**
     * @return the time in seconds after midnight at which the vehicle should begin traversing each
     * inter-stop segment ("hop") according to the original schedule.
     */
    public int getScheduledDepartureTime(int hop) {
        return scheduledDepartureTimes[hop];
    }

    /**
     * @return the time in seconds after midnight at which the vehicle should arrive at the end of
     * each inter-stop segment ("hop") according to the original schedule.
     */
    public int getScheduledArrivalTime(int hop) {
        if (scheduledArrivalTimes == null) {
            return scheduledDepartureTimes[hop + 1];
        } else {
            return scheduledArrivalTimes[hop];
        }
    }

    /**
     * @return the time in seconds after midnight at which the vehicle begins traversing each
     * inter-stop segment ("hop").
     */
    public int getDepartureTime(int hop) {
        if (departureTimes == null) {
            return scheduledDepartureTimes[hop];
        } else {
            return departureTimes[hop];
        }
    }

    /**
     * @return the time in seconds after midnight at which the vehicle arrives at the end of each
     * inter-stop segment ("hop"). A null value indicates that all dwells are 0-length, and arrival
     * times are to be derived from the departure times array.
     */
    public int getArrivalTime(int hop) {
        if (departureTimes == null) {
            return getScheduledArrivalTime(hop);
        } else if (arrivalTimes == null) {
            return departureTimes[hop + 1];
        } else {
            return arrivalTimes[hop];
        }
    }

    /**
     * Trips may also have null headsigns, in which case we should fall back on a Timetable or
     * Pattern-level headsign. Such a string will be available when we give TripPatterns or
     * StopPatterns unique human readable route variant names, but a TripTimes currently does not
     * have a pointer to its enclosing timetable or pattern.
     */
    public String getHeadsign(int hop) {
        if (headsigns == null) {
            return trip.getTripHeadsign();
        } else {
            return headsigns[hop];
        }
    }

    /** @return the stop sequence number specified in the GTFS feed for the given stop on this trip. */
    public int getStopSequence(int stopIndex) {
        return stopSequences[stopIndex];
    }

    /**
     * @return the amount of time in seconds that the vehicle waits at the stop *before* traversing
     * each inter-stop segment ("hop"). It is undefined for hop 0, and at the end of a trip. A value
     * of -1 indicates such a range error.
     */
    public int getDwellTime(int hop) {
        if (hop <= 0 || hop >= getNumHops()) return -1;

        int arrivalTime   = getArrivalTime(hop - 1);
        int departureTime = getDepartureTime(hop);

        return departureTime - arrivalTime;
    }

    /**
     * @return the length of time time in seconds that it takes for the vehicle to traverse each
     * inter-stop segment ("hop"). It is undefined for hops that don't exist. A value of -1
     * indicates such a range error.
     */
    public int getRunningTime(int hop) {
        if (hop < 0 || hop >= getNumHops()) return -1;

        int arrivalTime   = getArrivalTime(hop);
        int departureTime = getDepartureTime(hop);

        if(departureTime == UNAVAILABLE) {
            return -1;
        }

        return arrivalTime - departureTime;
    }

    /** @return the difference between the scheduled and actual departure times for this hop. */
    public int getDepartureDelay(int hop) {
        return getDepartureTime(hop) - getScheduledDepartureTime(hop);
    }

    /** @return the difference between the scheduled and actual arrival times for this hop. */
    public int getArrivalDelay(int hop) {
        return getArrivalTime(hop) - getScheduledArrivalTime(hop);
    }

    /**
     * @return true if this TripTimes represents an unmodified, scheduled trip from a published
     * timetable or false if it is a updated, cancelled, or otherwise modified one.
     */
    public boolean isScheduled() {
        return departureTimes == null;
    }

    private String formatSeconds(int s) {
        int m = s / 60;
        s = s % 60;
        int h = m / 60;
        m = m % 60;
        return String.format("%02d:%02d:%02d", h, m, s);
    }

    /** Builds a string concisely representing all departure and arrival times in this TripTimes. */
    public String dumpTimes() {
        StringBuilder sb = new StringBuilder();
        int nHops = this.getNumHops();
        // compaction is multi-layered now
        //sb.append(arrivalTimes == null ? "C " : "U ");
        for (int hop=0; hop < nHops; hop++) {
            String s = String.format("(%d)%8s__%8s", hop, formatSeconds(this.getDepartureTime(hop)),
                    formatSeconds(this.getArrivalTime(hop)));
            sb.append(s);
        }
        return sb.toString();
    }

    /**
     * Request that this TripTimes be analyzed and its memory usage reduced if possible. Replaces
     * the arrivals array with null if all dwell times are zero.
     * @return whether or not compaction occurred.
     */
    public boolean compact() {
        return compactScheduledArrivalsAndDepartures() || compactArrivalsAndDepartures();
    }

    private boolean compactScheduledArrivalsAndDepartures() {
        if (scheduledArrivalTimes == null) return false;
        // use scheduledArrivalTimes to determine number of hops because scheduledDepartureTimes may
        // have grown by 1 due to successive compact/decompact operations
        int nHops = scheduledArrivalTimes.length;
        // dwell time is undefined for hop 0, because there is no arrival for hop -1
        for (int hop = 1; hop < nHops; hop++) {
            if (this.getDwellTime(hop) != 0) {
                LOG.trace("compact failed: nonzero dwell time before hop {}", hop);
                return false;
            }
        }
        // extend scheduledDepartureTimes array by 1 to hold final arrival time
        scheduledDepartureTimes = Arrays.copyOf(scheduledDepartureTimes, nHops+1);
        scheduledDepartureTimes[nHops] = scheduledArrivalTimes[nHops-1];
        scheduledArrivalTimes = null;
        return true;
    }

    public boolean compactArrivalsAndDepartures() {
        if (arrivalTimes == null || departureTimes == null) return false;
        // use arrivalTimes to determine number of hops because departureTimes may have grown by 1
        // due to successive compact/decompact operations
        int nHops = arrivalTimes.length;
        // dwell time is undefined for hop 0, because there is no arrival for hop -1
        for (int hop = 1; hop < nHops; hop++) {
            if (this.getDwellTime(hop) != 0) {
                LOG.trace("compact failed: nonzero dwell time before hop {}", hop);
                return false;
            }
        }
        // extend departureTimes array by 1 to hold final arrival time
        departureTimes = Arrays.copyOf(departureTimes, nHops+1);
        departureTimes[nHops] = arrivalTimes[nHops-1];
        arrivalTimes = null;
        return true;
    }

    /**
     * In GTFS, each trip on the same pattern can have different stop sequence numbers.
     * In practice they are often the same for all trips on the same pattern. This method
     * deduplicates identical lists of stop sequence numbers.
     */
    public void compactStopSequence(TripTimes firstTripTime) {
        if (firstTripTime.stopSequences == null || stopSequences == null) {
            return;
        } else if (firstTripTime.stopSequences.equals(stopSequences)) {
            stopSequences = firstTripTime.stopSequences;
        }
    }

    /**
     * When creating a scheduled TripTimes or wrapping it in updates, we could potentially imply
     * negative hop or dwell times. We really don't want those being used in routing.
     * This method check that all times are increasing, and issues warnings if this is not the case.
     * @return whether the times were found to be increasing.
     */
    public boolean timesIncreasing() {
        // iterate over the new tripTimes, checking that dwells and hops are positive
        int nHops = getNumHops();
        int prevArr = -1;
        for (int hop = 0; hop < nHops; hop++) {
            int dep = getDepartureTime(hop);
            int arr = getArrivalTime(hop);

            if (arr < dep) { // negative hop time
                LOG.error("Negative hop time in TripTimes at index {}.", hop);
                return false;
            }
            if (prevArr > dep) { // negative dwell time before this hop
                LOG.error("Negative dwell time in TripTimes at index {}.", hop);
                return false;
            }
            prevArr = arr;
        }
        return true;
    }

    /* STATIC METHODS TAKING TRIPTIMES AS ARGUMENTS */

    /**
     * Once a trip has been found departing or arriving at an appropriate time, check whether that
     * trip fits other restrictive search criteria such as bicycle and wheelchair accessibility
     * and transfers with minimum time or forbidden transfers.
     *
     * @param stopIndex
     */
    public boolean tripAcceptable(State state0, Stop currentStop, ServiceDay sd, boolean bicycle,
            int stopIndex, boolean boarding) {
        RoutingRequest options = state0.getOptions();
        Trip trip = this.getTrip();
        BannedStopSet banned = options.bannedTrips.get(trip.getId());
        if (banned != null) {
            if (banned.contains(stopIndex)) {
                return false;
            }
        }
        if (options.wheelchairAccessible && trip.getWheelchairAccessible() != 1) {
            return false;
        }
        if (bicycle && BikeAccess.fromTrip(trip) != BikeAccess.ALLOWED) {
            return false;
        }

        // Check transfer table rules
        if (state0.isEverBoarded()) {
            // This is not the first boarding, thus a transfer
            TransferTable transferTable = options.getRoutingContext().transferTable;
            // Get the transfer time
            int transferTime = transferTable.getTransferTime(state0.getPreviousStop(),
                    currentStop, state0.getPreviousTrip(), trip, boarding);
            // Check for minimum transfer time and forbidden transfers
            if (transferTime > 0) {
                // There is a minimum transfer time to make this transfer
                int hopIndex = stopIndex - (boarding ? 0 : 1);
                if (boarding) {
                    if (sd.secondsSinceMidnight(state0.getLastAlightedTimeSeconds())
                            + transferTime > getDepartureTime(hopIndex)) {
                        return false;
                    }
                } else {
                    if (sd.secondsSinceMidnight(state0.getLastAlightedTimeSeconds())
                            - transferTime < getArrivalTime(hopIndex)) {
                        return false;
                    }
                }
            } else if (transferTime == StopTransfer.FORBIDDEN_TRANSFER) {
                // This transfer is forbidden
                return false;
            }

            // Check whether back edge is TimedTransferEdge
            if (state0.getBackEdge() instanceof TimedTransferEdge) {
                // Transfer must be of type TIMED_TRANSFER
                if (transferTime != StopTransfer.TIMED_TRANSFER) {
                    return false;
                }
            }
        }

        return true;
    }

    /** Cancel this entire trip */
    public void cancel() {
        cancelled = true;
        departureTimes = new int[getNumHops()];
        Arrays.fill(departureTimes, UNAVAILABLE);
        arrivalTimes = departureTimes;
    }

    public void updateDepartureTime(int hop, int time) {
        if (departureTimes == null) createDepartureTimesArray();
        departureTimes[hop] = time;
    }

    public void updateDepartureDelay(int hop, int delay) {
        if (departureTimes == null) createDepartureTimesArray();
        departureTimes[hop] = getScheduledDepartureTime(hop) + delay;
    }

    public void updateArrivalTime(int hop, int time) {
        if (arrivalTimes == null) createArrivalTimesArray();
        arrivalTimes[hop] = time;
    }

    public void updateArrivalDelay(int hop, int delay) {
        if (arrivalTimes == null) createArrivalTimesArray();
        arrivalTimes[hop] = getScheduledArrivalTime(hop) + delay;
    }

    /** Create an array of departure times that's just a copy of the scheduled departure times */
    private void createDepartureTimesArray() {
        departureTimes = Arrays.copyOf(scheduledDepartureTimes, scheduledDepartureTimes.length);
    }

    /** Create an array of arrival times that's just a copy of the scheduled arrival times */
    private void createArrivalTimesArray() {
        if (scheduledArrivalTimes == null) {
            int size = scheduledDepartureTimes.length;  // Is this really the right value? Unsure...
            arrivalTimes = Arrays.copyOfRange(scheduledDepartureTimes, 1, size);
        } else {
            arrivalTimes = Arrays.copyOf(scheduledArrivalTimes, scheduledArrivalTimes.length);
        }
    }

    /* NESTED STATIC CLASSES */

    /** Used for sorting an array of StopTimes based on arrivals for a specific hop. */
    @AllArgsConstructor
    public static class ArrivalsComparator implements Comparator<TripTimes> {
        final int hop;
        @Override public int compare(TripTimes tt1, TripTimes tt2) {
            return tt1.getArrivalTime(hop) - tt2.getArrivalTime(hop);
        }
    }

    /** Used for sorting an array of StopTimes based on departures for a specific hop. */
    @AllArgsConstructor
    public static class DeparturesComparator implements Comparator<TripTimes> {
        final int hop;
        @Override public int compare(TripTimes tt1, TripTimes tt2) {
            return tt1.getDepartureTime(hop) - tt2.getDepartureTime(hop);
        }
    }

    /* Allow sorting TripTimes based on first departure time. */
    @Override
    public int compareTo(TripTimes other) {
        return this.getDepartureTime(0) - other.getDepartureTime(0);
    }
}
