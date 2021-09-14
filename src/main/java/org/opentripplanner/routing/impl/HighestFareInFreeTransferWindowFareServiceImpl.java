package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Currency;
import java.util.List;

import org.opentripplanner.routing.core.Fare;
import org.opentripplanner.routing.core.FareRuleSet;
import org.opentripplanner.routing.core.Fare.FareType;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.PatternInterlineDwell;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HighestFareInFreeTransferWindowFareServiceImpl extends DefaultFareServiceImpl {
    private final boolean analyzeInterlinedTransfers;
    private final int freeTransferWindowInMinutes;

    public HighestFareInFreeTransferWindowFareServiceImpl(
        Collection<FareRuleSet> regularFareRules, int freeTransferWindowInMinutes, boolean analyzeInterlinedTransfers
    ) {
        addFareRules(FareType.regular, regularFareRules);
        this.freeTransferWindowInMinutes = freeTransferWindowInMinutes;
        this.analyzeInterlinedTransfers = analyzeInterlinedTransfers;
    }

    private static final long serialVersionUID = 20120229L;
    @SuppressWarnings("unused")
    private static final Logger LOG = LoggerFactory.getLogger(HighestFareInFreeTransferWindowFareServiceImpl.class);

    /**
     * The fare calculation is designed to charge the rider incrementally as they use each service. The total cost of
     * the itinerary will be equal to the leg of the journey that had the maximum fare. Additionally, a free transfer
     * window is taken into account that will apply the current highest cost within the free transfer window and then
     * reset to a new window and calculate additional free transfers from there.
     */
    @Override
    protected boolean populateFare(Fare fare, Currency currency, FareType fareType, List<Ride> rides,
                                   Collection<FareRuleSet> fareRules) {
        float cost = 0;
        float currentTransferWindowCost = 0;
        // The initial value of -1 indicates that the free transfer window end time has not yet been set
        long freeTransferWindowEndTimeEpochSeconds = -1;
        for (Ride ride : rides) {
            List<Ride> leg = new ArrayList<>();
            leg.add(ride);
            float rideCost = calculateCost(fareType, leg, fareRules);

            if (ride.startTime > freeTransferWindowEndTimeEpochSeconds) {
                // free transfer window has expired or has not yet been initialized. Reset some items and add to the
                // overall cost. This is fine to do if the free transfer window hasn't been initialized since the
                // overall cost will be 0.
                cost += currentTransferWindowCost;
                // reset current window cost
                currentTransferWindowCost = 0;
                // reset transfer window end time to trigger recalculation in next block
                freeTransferWindowEndTimeEpochSeconds = -1;
            }

            // recalculate free transfer window if needed
            if (freeTransferWindowEndTimeEpochSeconds == -1) {
                // the new transfer window end time should be calculated by adding the ride's start time (which is in
                // seconds past the epoch) and the number of equivalent seconds in the free transfer window minutes.
                freeTransferWindowEndTimeEpochSeconds = ride.startTime + freeTransferWindowInMinutes * 60;
            }

            currentTransferWindowCost = Float.max(currentTransferWindowCost, rideCost);
        }
        cost += currentTransferWindowCost;
        if (cost < Float.POSITIVE_INFINITY) fare.addFare(fareType, getMoney(currency, cost));
        return cost > 0 && cost < Float.POSITIVE_INFINITY;
    }

    /**
     * Returns true if configured to analyze interlined transfers and the previous edge was an interline transfer.
     */
    @Override
    protected boolean createRideDueToInterlining(State state, Ride ride) {
        if (!analyzeInterlinedTransfers) {
            return false;
        }

        // if the previous state's back edge was a PatternInterlineDwell, then a new ride should be created
        return state.getBackState().backEdge instanceof PatternInterlineDwell;
    }
}
