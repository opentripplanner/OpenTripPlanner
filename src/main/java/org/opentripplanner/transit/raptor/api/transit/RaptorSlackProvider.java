package org.opentripplanner.transit.raptor.api.transit;


/**
 * Responsible for providing {@code boardSlack}, {@code alightSlack} and {@code transferSlack}.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public interface RaptorSlackProvider<T extends RaptorTripSchedule> {

    /**
     * The transfer-slack (duration time in seconds) to add between transfers. This
     * is in addition to {@link #boardSlack(RaptorTripPattern)} and
     * {@link #alightSlack(RaptorTripPattern)}.
     * <p>
     * Unit: seconds, default value is 2 minutes.
     */
    default int transferSlack() { return 120; }

    /**
     * The board-slack (duration time in seconds) to add to the stop arrival time,
     * before boarding the given trip pattern.
     * <p>
     * Implementation notes: In a forward-search the pattern is known, but not the trip
     * (You must calculate the earliest-bord-time before boarding).
     * <p>
     * Unit: seconds, default value is 0.
     */
    default int boardSlack(RaptorTripPattern pattern) { return 0; }

    /**
     * The alight-slack (duration time in seconds) to add to the trip alight time for
     * the given pattern when calculating the the stop-arrival-time.
     * <p>
     * Implementation notes: In a reverse-search the pattern is known, but not the trip
     * (You must calculate the latest-alight-time before finding the trip-by-arriving-time).
     * <p>
     * Unit: seconds, default value is 0.
     */
    default int alightSlack(RaptorTripPattern pattern) { return 0; }
}
