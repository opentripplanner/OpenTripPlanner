package org.opentripplanner.routing.algorithm.raptor.transit;

import org.opentripplanner.routing.algorithm.raptor.transit.request.TripPatternForDates;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;

import java.util.Map;


/**
 * This class provides transferSlack, boardSlack and alightSlack for the Raptor algorithm.
 * <p>
 * Implementation notes: The board-slack and alight-slack is kept in an array, indexed by the mode
 * ordinal, and not in a {@link Map}, because it is faster. The board-slack and alight-slack lookup
 * is done for every strop-arrival computation, and should be as fast as possible.
 */
public final class SlackProvider implements RaptorSlackProvider {
    /**
     * Keep a list of board-slack values for each mode.
     */
    private final int[] boardSlack;

    /**
     * Keep a list of alight-slack values for each mode.
     *
     */
    private final int[] alightSlack;

    /**
     * A constant value is used for alight slack.
     */
    private final int transferSlack;


    public SlackProvider(
            int transferSlack,
            int defaultBoardSlack,
            Map<TraverseMode, Integer> modeBoardSlack,
            int defaultAlightSlack,
            Map<TraverseMode, Integer> modeAlightSlack
    ) {
        this.transferSlack = transferSlack;
        this.boardSlack = slackByMode(modeBoardSlack, defaultBoardSlack);
        this.alightSlack = slackByMode(modeAlightSlack, defaultAlightSlack);
    }

    @Override
    public int boardSlack(RaptorTripPattern pattern) {
        return boardSlack[index(pattern)];
    }

    @Override
    public int alightSlack(RaptorTripPattern pattern) {
        return alightSlack[index(pattern)];
    }

    @Override
    public int transferSlack() {
        return transferSlack;
    }


    /* private methods */

    /**
     * Return the trip-pattern ordinal as an index.
     */
    private static int index(RaptorTripPattern pattern) {
        return ((TripPatternForDates)pattern).getTripPattern().getTransitMode().ordinal();
    }

    private static int[] slackByMode(Map<TraverseMode, Integer> modeSlack, int defaultSlack) {
        int[] result = new int[TraverseMode.values().length];
        for (TraverseMode mode : TraverseMode.values()) {
            result[mode.ordinal()] = modeSlack.getOrDefault(mode, defaultSlack) ;
        }
        return result;
    }
}
