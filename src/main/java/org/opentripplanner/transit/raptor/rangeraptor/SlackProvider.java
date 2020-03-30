package org.opentripplanner.transit.raptor.rangeraptor;


import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

/**
 * Responsible for providing {@code boardSlack} and {@code alightSlack} to the Raptor algorithm -
 * to the worker implementation. This interface is used internally in Raptor and should not be
 * confused with the {@link org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider}
 * <p>
 * It should incorporate all kind of slacks from the transit layer
 * into board-slack and alight-slack. An example is that the transfer-slack (in transit domain)
 * can be added to the board-slack (in Raptor) assuming the transfer-slack is a constant.
 * <p>
 * The {@code SlackProvider} is also responsible for providing the correct slack according to
 * the search direction. There is two implementations of this interface:
 * <ul>
 *     <li>{@link org.opentripplanner.transit.raptor.rangeraptor.transit.ForwardSlackProvider}
 *     <li>{@link org.opentripplanner.transit.raptor.rangeraptor.transit.ReverseSlackProvider}
 * </ul>
 */
public interface SlackProvider {

    /**
     * Set the trip pattern to use when finding the {@code boardSlack} and {@code alightSlack}.
     */
    void setCurrentPattern(RaptorTripPattern pattern);

    /**
     * The board-slack (duration time in seconds) to add to the stop arrival time,
     * before boarding the given trip pattern.
     * <p>
     * Implementation notes: In a forward-search the pattern is known, but not the trip.
     * <p>
     * Unit: seconds.
     */
    int boardSlack();

    /**
     * The alight-slack (duration time in seconds) to add to the trip alight time for
     * the given pattern when calculating the the stop-arrival-time.stop arrival time.
     * <p>
     * Implementation notes: In a reverse-search the pattern is known, but not the trip.
     * <p>
     * Unit: seconds.
     */
    int alightSlack();
}
