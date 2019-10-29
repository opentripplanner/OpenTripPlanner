package org.opentripplanner.transit.raptor.rangeraptor.standard.configure;

import org.opentripplanner.transit.raptor.api.request.RangeRaptorProfile;
import org.opentripplanner.transit.raptor.rangeraptor.transit.SearchContext;

import static org.opentripplanner.transit.raptor.api.request.RangeRaptorProfile.NO_WAIT_BEST_TIME;
import static org.opentripplanner.transit.raptor.api.request.RangeRaptorProfile.NO_WAIT_STD;


/**
 * This class verify that the request is valid in the context of Standard Range Raptor.
 */
class VerifyRequestIsValid  {
    private final SearchContext<?> context;


    VerifyRequestIsValid(SearchContext<?> context) {
        this.context = context;
    }

    void verify() {
        verifyNoWaitWorkersIsOneIterationOnly();
    }


    /* private methods */

    private void verifyNoWaitWorkersIsOneIterationOnly() {
        verify(noWaitWorker() && !oneIteration(), "The profile %s is only defined for one iteration.", profile());
    }

    private void verify(boolean condition, String format, Object... args) {
        if(condition) {
            throw new IllegalArgumentException(String.format(format, args));
        }
    }

    private RangeRaptorProfile profile() {
        return context.profile();
    }

    private boolean noWaitWorker() {
        return profile().isOneOf(NO_WAIT_STD, NO_WAIT_BEST_TIME);
    }

    private boolean oneIteration() {
        return context.calculator().oneIterationOnly();
    }

}
