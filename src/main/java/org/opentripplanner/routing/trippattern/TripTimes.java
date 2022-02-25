package org.opentripplanner.routing.trippattern;

import com.google.common.hash.HashCode;
import com.google.common.hash.HashFunction;
import com.google.common.hash.Hasher;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.StopPattern;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.TripPattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A TripTimes represents the arrival and departure times for a single trip in an Timetable. It is carried
 * along by States when routing to ensure that they have a consistent, fast view of the trip when
 * realtime updates have been applied. All times are expressed as seconds since midnight (as in GTFS).
 */
public class TripTimes implements Serializable, Comparable<TripTimes> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOG = LoggerFactory.getLogger(TripTimes.class);

    /**
     * This allows re-using the same scheduled arrival and departure time arrays for many
     * different TripTimes. It is also used in materializing frequency-based TripTimes.
     */
    private int timeShift;

    /** The trips whose arrivals and departures are represented by this TripTimes */
    private final Trip trip;

    // not final because these are set later, after TripTimes construction.
    private int serviceCode = -1;

    /**
     * Both trip_headsign and stop_headsign (per stop on a particular trip) are optional GTFS
     * fields. If the headsigns array is null, we will report the trip_headsign (which may also
     * be null) at every stop on the trip. If all the stop_headsigns are the same as the
     * trip_headsign we may also set the headsigns array to null to save space.
     * Field is private to force use of the getter method which does the necessary fallbacks.
     */
    private final String[] headsigns;

    /**
     * Contains a list of via names for each stop.
     * This field provides info about intermediate stops between current stop and final trip destination.
     * This is 2D array since there can be more than one via name/stop per each record in stop sequence).
     * This is mapped from NeTEx DestinationDisplay.vias. No GTFS mapping at the moment.
     * Outer array may be null if there are no vias in stop sequence. Inner array may be null if
     * there are no vias for particular stop. This is done in order to save space.
     * Field is private to force use of the getter method which does the necessary fallbacks.
     */
    private final String[][] headsignVias;

    /**
     * The time in seconds after midnight at which the vehicle should arrive at each stop according
     * to the original schedule.
     */
    private final int[] scheduledArrivalTimes;

    /**
     * The time in seconds after midnight at which the vehicle should leave each stop according
     * to the original schedule.
     */
    private final int[] scheduledDepartureTimes;

    /**
     * The time in seconds after midnight at which the vehicle arrives at each stop, accounting for
     * any real-time updates. Non-final to allow updates.
     */
    private int[] arrivalTimes;

    /**
     * The time in seconds after midnight at which the vehicle leaves each stop, accounting for
     * any real-time updates. Non-final to allow updates.
     */
    private int[] departureTimes;

    /**
     * Flag to indicate that the stop has been passed without removing arrival/departure-times - i.e. "estimates" are
     * actual times, no longer estimates.
     *
     * This is only for API-purposes. Non-final to allow updates.
     */
    private boolean[] recordedStops;

    /**
     * Whether each stop has been cancelled by realtime updates (in the most recent derived realtime StopPattern).
     * The cancellation information here need not match the information in the StopPattern for this TripTimes.
     * This is somewhat redundant: it duplicates cancellation information found in the {@link StopPattern} of the new
     * {@link TripPattern} created by the realtime update. The use case is displaying cancellation information when
     * this original (non-real-time) pattern is queried, even though this original pattern allows pickup and dropoff
     * at the canceled stop. This is only for API purposes and does not affect routing.
     *
     * // TODO This seems to only be changed after a new TripTimes has been created by the realtime
     * // TODO updater and the scheduled TripTimes is never changed. We should clean this up
     * // TODO when refactoring the realtime model. Preferably so that a reference is kept between
     * // TODO the realtime data and the scheduled data, so that data is not duplicated
     *
     * Non-final to allow updates.
     */
    private boolean[] cancelledStops;

    /**
     * Flag to indicate inaccurate predictions on each stop. Non-final to allow updates.
     */
    private boolean[] predictionInaccurateOnStops;

    private final List<BookingInfo> dropOffBookingInfos;

    private final List<BookingInfo> pickupBookingInfos;

    /**
     * These are the GTFS stop sequence numbers, which show the order in which the vehicle visits
     * the stops. Despite the face that the StopPattern or TripPattern enclosing this TripTimes
     * provides an ordered list of Stops, the original stop sequence numbers may still be needed for
     * matching with GTFS-RT update messages. Unfortunately, each individual trip can have totally
     * different sequence numbers for the same stops, so we need to store them at the individual
     * trip level. An effort is made to re-use the sequence number arrays when they are the same
     * across different trips in the same pattern.
     */
    private final int[] originalGtfsStopSequence;

    /**
     * The real-time state of this TripTimes.
     */
    private RealTimeState realTimeState = RealTimeState.SCHEDULED;

    /** A Set of stop indexes that are marked as timepoints in the GTFS input. */
    private final BitSet timepoints;

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The provided stopTimes are assumed to be pre-filtered, valid, and monotonically increasing.
     * The non-interpolated stoptimes should already be marked at timepoints by a previous filtering step.
     */
    public TripTimes(final Trip trip, final Collection<StopTime> stopTimes, final Deduplicator deduplicator) {
        this.trip = trip;
        final int nStops = stopTimes.size();
        final int[] departures = new int[nStops];
        final int[] arrivals   = new int[nStops];
        final int[] sequences  = new int[nStops];
        final BitSet timepoints = new BitSet(nStops);
        // Times are always shifted to zero. This is essential for frequencies and deduplication.
        this.timeShift = stopTimes.iterator().next().getArrivalTime();
        final List<BookingInfo> dropOffBookingInfos = new ArrayList<>();
        final List<BookingInfo> pickupBookingInfos = new ArrayList<>();
        int s = 0;
        for (final StopTime st : stopTimes) {
            departures[s] = st.getDepartureTime() - timeShift;
            arrivals[s] = st.getArrivalTime() - timeShift;
            sequences[s] = st.getStopSequence();
            timepoints.set(s, st.getTimepoint() == 1);

            dropOffBookingInfos.add(st.getDropOffBookingInfo());
            pickupBookingInfos.add(st.getPickupBookingInfo());
            s++;
        }
        this.scheduledDepartureTimes = deduplicator.deduplicateIntArray(departures);
        this.scheduledArrivalTimes = deduplicator.deduplicateIntArray(arrivals);
        this.originalGtfsStopSequence = deduplicator.deduplicateIntArray(sequences);
        this.headsigns = deduplicator.deduplicateStringArray(makeHeadsignsArray(stopTimes));
        this.headsignVias = deduplicator.deduplicateString2DArray(makeHeadsignViasArray(stopTimes));

        this.dropOffBookingInfos = deduplicator.deduplicateImmutableList(BookingInfo.class, dropOffBookingInfos);
        this.pickupBookingInfos = deduplicator.deduplicateImmutableList(BookingInfo.class, pickupBookingInfos);
        // We set these to null to indicate that this is a non-updated/scheduled TripTimes.
        // We cannot point to the scheduled times because they are shifted, and updated times are not.
        this.arrivalTimes = null;
        this.departureTimes = null;
        this.recordedStops = null;
        this.cancelledStops = null;
        this.timepoints = deduplicator.deduplicateBitSet(timepoints);
        LOG.trace("trip {} has timepoint at indexes {}", trip, timepoints);
    }

    /** This copy constructor does not copy the actual times, only the scheduled times. */
    public TripTimes(final TripTimes object) {
        this.timeShift = object.timeShift;
        this.trip = object.trip;
        this.serviceCode = object.serviceCode;
        this.headsigns = object.headsigns;
        this.headsignVias = object.headsignVias;
        this.scheduledArrivalTimes = object.scheduledArrivalTimes;
        this.scheduledDepartureTimes = object.scheduledDepartureTimes;
        this.arrivalTimes = null;
        this.departureTimes = null;
        this.recordedStops = object.recordedStops;
        this.predictionInaccurateOnStops = object.predictionInaccurateOnStops;
        this.pickupBookingInfos = object.pickupBookingInfos;
        this.dropOffBookingInfos = object.dropOffBookingInfos;
        this.originalGtfsStopSequence = object.originalGtfsStopSequence;
        this.realTimeState = object.realTimeState;
        this.timepoints = object.timepoints;
    }

    public void setServiceCode(int serviceCode) {
        this.serviceCode = serviceCode;
    }

    /**
     * @return either an array of headsigns (one for each stop on this trip) or null if the
     * headsign is the same at all stops (including null) and can be found in the Trip object.
     */
    private String[] makeHeadsignsArray(final Collection<StopTime> stopTimes) {
        final String tripHeadsign = trip.getTripHeadsign();
        boolean useStopHeadsigns = false;
        if (tripHeadsign == null) {
            useStopHeadsigns = true;
        } else {
            for (final StopTime st : stopTimes) {
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
        final String[] hs = new String[stopTimes.size()];
        for (final StopTime st : stopTimes) {
            final String headsign = st.getStopHeadsign();
            hs[i++] = headsign;
            if (headsign != null) allNull = false;
        }
        if (allNull) {
            return null;
        } else {
            return hs;
        }
    }


    /**
     * Create 2D String array for via names for each stop in sequence.
     * @return May be null if no vias are present in stop sequence.
     */
    private String[][] makeHeadsignViasArray(final Collection<StopTime> stopTimes) {

        if (stopTimes.stream().allMatch(st -> st.getHeadsignVias() == null || st.getHeadsignVias().isEmpty())) {
            return null;
        }

        String[][] vias = new String[stopTimes.size()][];

        int i = 0;
        for (final StopTime st : stopTimes) {
            if (st.getHeadsignVias() == null) {
                vias[i] = EMPTY_STRING_ARRAY;
                continue;
            }

            vias[i] = st.getHeadsignVias().toArray(EMPTY_STRING_ARRAY);
            i++;
        }

        return vias;
    }

    /**
     * Trips may also have null headsigns, in which case we should fall back on a Timetable or
     * Pattern-level headsign. Such a string will be available when we give TripPatterns or
     * StopPatterns unique human readable route variant names, but a TripTimes currently does not
     * have a pointer to its enclosing timetable or pattern.
     */
    public String getHeadsign(final int stop) {
        if (headsigns == null) {
            return getTrip().getTripHeadsign();
        } else {
            return headsigns[stop];
        }
    }

    /**
     * Return list of via names per particular stop.
     * This field provides info about intermediate stops between current stop and final trip destination.
     * Mapped from NeTEx DestinationDisplay.vias. No GTFS mapping at the moment.
     *
     * @return Empty list if there are no vias registered for a stop.
     */
    public List<String> getVia(final int stop) {
        if (headsignVias == null || headsignVias[stop] == null) {
            return List.of();
        }
        return List.of(headsignVias[stop]);
    }

    /** @return the time in seconds after midnight that the vehicle arrives at the stop. */
    public int getScheduledArrivalTime(final int stop) {
        return scheduledArrivalTimes[stop] + timeShift;
    }

    /** @return the amount of time in seconds that the vehicle waits at the stop. */
    public int getScheduledDepartureTime(final int stop) {
        return scheduledDepartureTimes[stop] + timeShift;
    }

    /**
     * Return an integer which can be used to sort TripTimes in order of departure/arrivals.
     * <p>
     * This sorted trip times is used to search for trips. OTP assume one trip do NOT pass another
     * trip down the line.
     */
    public int sortIndex() {
        return getArrivalTime(0);
    }

    /** @return the time in seconds after midnight that the vehicle arrives at the stop. */
    public int getArrivalTime(final int stop) {
        if (arrivalTimes == null) { return getScheduledArrivalTime(stop); }
        else return arrivalTimes[stop]; // updated times are not time shifted.
    }

    /** @return the amount of time in seconds that the vehicle waits at the stop. */
    public int getDepartureTime(final int stop) {
        if (departureTimes == null) { return getScheduledDepartureTime(stop); }
        else return departureTimes[stop]; // updated times are not time shifted.
    }

    /** @return the difference between the scheduled and actual arrival times at this stop. */
    public int getArrivalDelay(final int stop) {
        return getArrivalTime(stop) - (scheduledArrivalTimes[stop] + timeShift);
    }

    /** @return the difference between the scheduled and actual departure times at this stop. */
    public int getDepartureDelay(final int stop) {
        return getDepartureTime(stop) - (scheduledDepartureTimes[stop] + timeShift);
    }

    public void setRecorded(int stop, boolean recorded) {
        prepareForRealTimeUpdates();
        recordedStops[stop] = recorded;
    }

    public void setCancelled(int stop) {
        prepareForRealTimeUpdates();
        cancelledStops[stop] = true;
    }

    public boolean isCancelledStop(int stop) {
        if (cancelledStops == null) {
            return false;
        }
        return cancelledStops[stop];
    }

    // TODO OTP2 - Unused, but will be used by Transmodel API
    public boolean isRecordedStop(int stop) {
        if (recordedStops == null) {
            return false;
        }
        return recordedStops[stop];
    }

    //Is prediction for single stop inaccurate
    public void setPredictionInaccurate(int stop, boolean predictionInaccurate) {
        prepareForRealTimeUpdates();
        predictionInaccurateOnStops[stop] = predictionInaccurate;
    }

    // TODO OTP2 - Unused, but will be used by Transmodel API
    public boolean isPredictionInaccurate(int stop) {
        if (predictionInaccurateOnStops == null) {
            return false;
        }
        return predictionInaccurateOnStops[stop];
    }

    public BookingInfo getDropOffBookingInfo(int stop) {
        return dropOffBookingInfos.get(stop);
    }

    public BookingInfo getPickupBookingInfo(int stop) {
        return pickupBookingInfos.get(stop);
    }

    /**
     * @return true if this TripTimes represents an unmodified, scheduled trip from a published
     *         timetable or false if it is a updated, cancelled, or otherwise modified one. This
     *         method differs from {@link #getRealTimeState()} in that it checks whether real-time
     *         information is actually available in this TripTimes.
     */
    public boolean isScheduled() {
        return realTimeState == RealTimeState.SCHEDULED;
    }

    /**
     * @return true if this TripTimes is canceled
     */
    public boolean isCanceled() {
        return realTimeState == RealTimeState.CANCELED;
    }

    /**
     * @return the real-time state of this TripTimes
     */
    public RealTimeState getRealTimeState() {
        return realTimeState;
    }

    public void setRealTimeState(final RealTimeState realTimeState) {
        this.realTimeState = realTimeState;
    }

    /**
     * When creating a scheduled TripTimes or wrapping it in updates, we could potentially imply
     * negative running or dwell times. We really don't want those being used in routing.
     * This method check that all times are increasing, and logs warnings if this is not the case.
     * @return whether the times were found to be increasing.
     */
    public boolean timesIncreasing() {
        final int nStops = scheduledArrivalTimes.length;
        int prevDep = -1;
        for (int s = 0; s < nStops; s++) {
            final int arr = getArrivalTime(s);
            final int dep = getDepartureTime(s);

            if (dep < arr) {
                LOG.warn("Negative dwell time in TripTimes at stop index {}.", s);
                return false;
            }
            if (prevDep > arr) {
                LOG.warn("Negative running time in TripTimes after stop index {}.", s);
                return false;
            }
            prevDep = dep;
        }
        return true;
    }

    /** Cancel this entire trip */
    public void cancelTrip() {
        realTimeState = RealTimeState.CANCELED;
    }

    public void updateDepartureTime(final int stop, final int time) {
        prepareForRealTimeUpdates();
        departureTimes[stop] = time;
    }

    public void updateDepartureDelay(final int stop, final int delay) {
        prepareForRealTimeUpdates();
        departureTimes[stop] = scheduledDepartureTimes[stop] + timeShift + delay;
    }

    public void updateArrivalTime(final int stop, final int time) {
        prepareForRealTimeUpdates();
        arrivalTimes[stop] = time;
    }

    public void updateArrivalDelay(final int stop, final int delay) {
        prepareForRealTimeUpdates();
        arrivalTimes[stop] = scheduledArrivalTimes[stop] + timeShift + delay;
    }

    /**
     * If they don't already exist, create arrays for updated arrival and departure times
     * that are just time-shifted copies of the zero-based scheduled departure times.
     *
     * Also sets the realtime state to UPDATED.
     */
    private void prepareForRealTimeUpdates() {
        if (arrivalTimes == null) {
            this.arrivalTimes = Arrays.copyOf(scheduledArrivalTimes, scheduledArrivalTimes.length);
            this.departureTimes = Arrays.copyOf(scheduledDepartureTimes, scheduledDepartureTimes.length);
            this.recordedStops = new boolean[arrivalTimes.length];
            this.cancelledStops = new boolean[arrivalTimes.length];
            this.predictionInaccurateOnStops = new boolean[arrivalTimes.length];
            for (int i = 0; i < arrivalTimes.length; i++) {
                arrivalTimes[i] += timeShift;
                departureTimes[i] += timeShift;
                recordedStops[i] = false;
                cancelledStops[i] = false;
                predictionInaccurateOnStops[i] = false;
            }

            // Update the real-time state
            realTimeState = RealTimeState.UPDATED;
        }
    }

    public int getNumStops () {
        return scheduledArrivalTimes.length;
    }

    /** Sort TripTimes based on first departure time. */
    @Override
    public int compareTo(final TripTimes other) {
        return this.getDepartureTime(0) - other.getDepartureTime(0);
    }

   /**
    * Returns a time-shifted copy of this TripTimes in which the vehicle passes the given stop
    * index (not stop sequence number) at the given time. We only have a mechanism to shift the
    * scheduled stoptimes, not the real-time stoptimes. Therefore, this only works on trips
    * without updates for now (frequency trips don't have updates).
    */
    public TripTimes timeShift (final int stop, final int time, final boolean depart) {
        if (arrivalTimes != null || departureTimes != null) { return null; }
        final TripTimes shifted = new TripTimes(this);
        // Adjust 0-based times to match desired stoptime.
        final int shift = time - (depart ? getDepartureTime(stop) : getArrivalTime(stop));
        // existing shift should usually (always?) be 0 on freqs
        shifted.timeShift = shifted.timeShift + shift;
        return shifted;
    }

    /** Just to create uniform getter-syntax across the whole public interface of TripTimes. */
    public int getOriginalGtfsStopSequence(final int stop) {
        return originalGtfsStopSequence[stop];
    }

    /** @return whether or not stopIndex is considered a timepoint in this TripTimes. */
    public boolean isTimepoint(final int stopIndex) {
        return timepoints.get(stopIndex);
    }

    /**
     * Hash the scheduled arrival/departure times. Used in creating stable IDs for trips across GTFS feed versions.
     * Use hops rather than stops because:
     * a) arrival at stop zero and departure from last stop are irrelevant
     * b) this hash function needs to stay stable when users switch from 0.10.x to 1.0
     */
    public HashCode semanticHash(final HashFunction hashFunction) {
        final Hasher hasher = hashFunction.newHasher();
        for (int hop = 0; hop < getNumStops() - 1; hop++) {
            hasher.putInt(getScheduledDepartureTime(hop));
            hasher.putInt(getScheduledArrivalTime(hop + 1));
        }
        return hasher.hash();
    }

    /** The code for the service on which this trip runs. For departure search optimizations. */
    public int getServiceCode() {
        return serviceCode;
    }

    /** The trips whose arrivals and departures are represented by this TripTimes */
    public Trip getTrip() {
        return trip;
    }
}
