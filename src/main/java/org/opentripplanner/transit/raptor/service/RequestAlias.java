package org.opentripplanner.transit.raptor.service;

import org.opentripplanner.transit.raptor.api.request.RaptorRequest;

public class RequestAlias {
    public static String alias(RaptorRequest<?> request, boolean serviceMultithreaded) {
        boolean multithreaded = serviceMultithreaded && request.runInParallel();
        String alias = request.profile().abbreviation();

        if (request.searchInReverse()) {
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
        if (request.useTransfersStopFilter()) {
            // Heuristic used to generate stop filter based on number of transfers
            alias += "-SF";
        }
        return alias;
    }
}
