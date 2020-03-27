package org.opentripplanner.transit.raptor.service;

import org.opentripplanner.transit.raptor.api.request.RaptorRequest;

public class RequestAlias {
    public static String alias(RaptorRequest<?> request, boolean serviceMultithreaded) {
        boolean multithreaded = serviceMultithreaded && request.runInParallel();
        String alias = request.profile().abbreviation();

        if (request.searchDirection().isInReverse()) {
            alias += "-Rev";
        }
        if (multithreaded) {
            // Run search in parallel
            alias += "-LL";
        }
        if (request.useDestinationPruning()) {
            // Heuristic to prune on pareto optimal Destination arrivals
            alias += "-DP";
        }
        return alias;
    }
}
