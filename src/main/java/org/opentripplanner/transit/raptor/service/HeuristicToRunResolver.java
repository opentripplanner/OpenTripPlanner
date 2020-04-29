package org.opentripplanner.transit.raptor.service;

import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.SearchParams;

import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.STANDARD;

class HeuristicToRunResolver {

    /**
     * Create and prepare heuristic search (both FORWARD and REVERSE) based on optimizations and
     * input search parameters. This is done for Standard and Multi-criteria profiles only.
     */
    static void resolveHeuristicToRunBasedOnOptimizationsAndSearchParameters(
            RaptorRequest<?> req,
            Runnable turnFwdOn,
            Runnable turnRevOn
    ) {
        if (!req.profile().isOneOf(STANDARD, MULTI_CRITERIA)) {
            return;
        }
        boolean forward = false;
        boolean reverse = false;

        // Alias for continence
        final SearchParams s = req.searchParams();

        if (req.profile().is(MULTI_CRITERIA)) {
            // REV heuristics is required to do destination pruning
            if (req.useDestinationPruning()) {
                reverse = true;
            }
        }

        // Use REV heuristics to find EAT (earliest arrival time), EAT is required by the search.
        if (!s.isEarliestDepartureTimeSet()) {
            reverse = true;
        }

        // Use FWD heuristics to find LAT(latest arrival time) if REV heuristics is enabled
        if (!s.isLatestArrivalTimeSet() && reverse) {
            forward = true;
        }

        // Use either REV/FWD heuristics to calculate search window
        if (!s.isSearchWindowSet() && !reverse) {
            forward = true;
        }

        // Callback to set result
        if(forward) { turnFwdOn.run(); }
        if(reverse) { turnRevOn.run(); }
    }
}
