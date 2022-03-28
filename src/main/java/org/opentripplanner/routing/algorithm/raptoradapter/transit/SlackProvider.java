package org.opentripplanner.routing.algorithm.raptoradapter.transit;

import java.util.Map;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.request.TripPatternForDates;
import org.opentripplanner.transit.raptor.api.transit.RaptorSlackProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripPattern;


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
     * Kept in an array for performance
     */
    private final int[] alightSlack;

    /**
     * A constant value is used for alight slack.
     */
    private final int transferSlack;


    public SlackProvider(
            int transferSlack,
            int defaultBoardSlack,
            Map<TransitMode, Integer> modeBoardSlack,
            int defaultAlightSlack,
            Map<TransitMode, Integer> modeAlightSlack
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

    private static int[] slackByMode(Map<TransitMode, Integer> modeSlack, int defaultSlack) {
        int[] result = new int[TransitMode.values().length];
        for (TransitMode mode : TransitMode.values()) {
            result[index(mode)] = modeSlack.getOrDefault(mode, defaultSlack) ;
        }
        return result;
    }

    private static int index(RaptorTripPattern pattern) {
        return index(((TripPatternForDates)pattern).getTripPattern().getTransitMode());
    }

    private static int index(final TransitMode mode) {
        return mode.ordinal();
    }
}
