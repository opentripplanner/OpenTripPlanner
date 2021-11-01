package org.opentripplanner.routing.algorithm.raptor.transit;

import java.util.EnumMap;
import java.util.Map;
import org.opentripplanner.model.TransitMode;
import org.opentripplanner.routing.algorithm.raptor.transit.request.TripPatternForDates;
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
     * Default boardSlack if an explicit value is not set for the mode.
     */
    private final int defaultBoardSlack;

    /**
     * Keep a list of board-slack values for each mode.
     */
    private final EnumMap<TransitMode, Integer> boardSlack;

    /**
     * Default alightSlack if an explicit value is not set for the mode.
     */
    private final int defaultAlightSlack;

    /**
     * Keep a list of alight-slack values for each mode.
     *
     */
    private final EnumMap<TransitMode, Integer> alightSlack;

    /**
     * A constant value is used for alight slack.
     */
    private final int transferSlack;


    public SlackProvider(
            int transferSlack,
            int defaultBoardSlack,
            EnumMap<TransitMode, Integer> modeBoardSlack,
            int defaultAlightSlack,
            EnumMap<TransitMode, Integer> modeAlightSlack
    ) {
        this.transferSlack = transferSlack;
        this.defaultBoardSlack = defaultBoardSlack;
        this.boardSlack = modeBoardSlack;
        this.defaultAlightSlack = defaultAlightSlack;
        this.alightSlack = modeAlightSlack;
    }

    @Override
    public int boardSlack(RaptorTripPattern pattern) {
        return boardSlack.getOrDefault(modeForPattern(pattern), defaultBoardSlack);
    }

    @Override
    public int alightSlack(RaptorTripPattern pattern) {
        return alightSlack.getOrDefault(modeForPattern(pattern), defaultAlightSlack);
    }

    @Override
    public int transferSlack() {
        return transferSlack;
    }


    /* private methods */

    /**
     * Return the mode for the trip pattern.
     */
    private static TransitMode modeForPattern(RaptorTripPattern pattern) {
        return ((TripPatternForDates)pattern).getTripPattern().getTransitMode();
    }
}
