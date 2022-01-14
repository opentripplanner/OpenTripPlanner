package org.opentripplanner.transit.raptor.rangeraptor.standard.configure;

import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MIN_TRAVEL_DURATION_BEST_TIME;

import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext;


/**
 * This class verify that the request is valid in the context of Standard Range Raptor.
 */
class VerifyRequestIsValid  {
    private final SearchContext<?> context;


    VerifyRequestIsValid(SearchContext<?> context) {
        this.context = context;
    }

    void verify() {
        verifyMinTravelDurationIsOneIterationOnly();
    }


    /* private methods */

    private void verifyMinTravelDurationIsOneIterationOnly() {
        verify(minTravelDurationStrategy() && !oneIteration(), "The profile %s is only defined for one iteration.", profile());
    }

    private void verify(boolean condition, String format, Object... args) {
        if(condition) {
            throw new IllegalArgumentException(String.format(format, args));
        }
    }

    private RaptorProfile profile() {
        return context.profile();
    }

    private boolean minTravelDurationStrategy() {
        return profile().isOneOf(MIN_TRAVEL_DURATION, MIN_TRAVEL_DURATION_BEST_TIME);
    }

    private boolean oneIteration() {
        return context.calculator().oneIterationOnly();
    }

}
