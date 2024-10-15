package org.opentripplanner.raptor.service;

import static org.opentripplanner.raptor.api.request.RaptorProfile.MULTI_CRITERIA;
import static org.opentripplanner.raptor.api.request.RaptorProfile.STANDARD;

import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.SearchParams;

class HeuristicToRunResolver {

  /**
   * Create and prepare heuristic search (both FORWARD and REVERSE) based on optimizations and input
   * search parameters. This is done for Standard and Multi-criteria profiles only.
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

    // Use REV heuristics to find EDT (earliest departure time), EDT is required by the search.
    if (!s.isEarliestDepartureTimeSet()) {
      reverse = true;
    }

    // Use either REV/FWD heuristics to calculate search window
    if (!s.isSearchWindowSet() && !reverse) {
      forward = true;
    }

    // Callback to set result
    if (forward) {
      turnFwdOn.run();
    }
    if (reverse) {
      turnRevOn.run();
    }
  }
}
