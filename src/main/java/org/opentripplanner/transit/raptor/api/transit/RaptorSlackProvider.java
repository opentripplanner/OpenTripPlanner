package org.opentripplanner.transit.raptor.api.transit;


/**
 * Responsible for providing {@code boardSlack}, {@code alightSlack} and {@code transferSlack}.
 */
public interface RaptorSlackProvider {

    /**
     * Return a default implementation witch can be used in unit tests. The
     * transfer-slack is 60 seconds, while the alight-slack and board-slack is 0 seconds.
     */
    static RaptorSlackProvider defaults() {
        return new RaptorSlackProvider() {
            @Override public int transferSlack() { return 60; }
            @Override public int boardSlack(RaptorTripPattern pattern) { return 0; }
            @Override public int alightSlack(RaptorTripPattern pattern) { return 0; }
        };
    }

    /**
     * The transfer-slack (duration time in seconds) to add between transfers. This
     * is in addition to {@link #boardSlack(RaptorTripPattern)} and
     * {@link #alightSlack(RaptorTripPattern)}.
     * <p>
     * Unit: seconds.
     */
    int transferSlack();

    /**
     * The board-slack (duration time in seconds) to add to the stop arrival time,
     * before boarding the given trip pattern.
     * <p>
     * Implementation notes: In a forward-search the pattern is known, but not the trip
     * (You must calculate the earliest-bord-time before boarding).
     * <p>
     * Unit: seconds.
     */
    int boardSlack(RaptorTripPattern pattern);

    /**
     * The alight-slack (duration time in seconds) to add to the trip alight time for
     * the given pattern when calculating the the stop-arrival-time.
     * <p>
     * Implementation notes: In a reverse-search the pattern is known, but not the trip
     * (You must calculate the latest-alight-time before finding the trip-by-arriving-time).
     * <p>
     * Unit: seconds.
     */
    int alightSlack(RaptorTripPattern pattern);
}
