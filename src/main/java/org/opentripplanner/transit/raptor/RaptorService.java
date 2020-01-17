package org.opentripplanner.transit.raptor;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.TuningParameters;
import org.opentripplanner.transit.raptor.api.transit.TransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.view.Heuristics;
import org.opentripplanner.transit.raptor.api.view.Worker;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.transit.raptor.rangeraptor.standard.heuristics.HeuristicSearch;
import org.opentripplanner.transit.raptor.service.DebugHeuristics;
import org.opentripplanner.transit.raptor.service.RequestAlias;

import java.util.BitSet;
import java.util.Collection;
import java.util.concurrent.Future;

import static org.opentripplanner.transit.raptor.api.request.Optimization.PARALLEL;
import static org.opentripplanner.transit.raptor.api.request.Optimization.PARETO_CHECK_AGAINST_DESTINATION;
import static org.opentripplanner.transit.raptor.api.request.Optimization.TRANSFERS_STOP_FILTER;
import static org.opentripplanner.transit.raptor.api.request.RaptorProfile.NO_WAIT_BEST_TIME;

/**
 * A service for performing Range Raptor routing request.
 *
 * @param <T> The TripSchedule type defined by the user of the raptor API.
 */
public class RaptorService<T extends RaptorTripSchedule> {
    private static final boolean FORWARD = true;
    private static final boolean REVERSE = false;

    private final RaptorConfig<T> config;

    public RaptorService(TuningParameters tuningParameters) {
        this.config = new RaptorConfig<>(tuningParameters);
    }

    public Collection<Path<T>> route(RaptorRequest<T> request, TransitDataProvider<T> transitData) {
        if (request.profile() == RaptorProfile.MULTI_CRITERIA) {
            return routeUsingMcWorker(transitData, request);
        } else {
            return routeUsingStdWorker(transitData, request);
        }
    }

    public void compareHeuristics(RaptorRequest<T> r1, RaptorRequest<T> r2, TransitDataProvider<T> transitData) {
        HeuristicSearch<T> h1 = config.createHeuristicSearch(transitData, r1);
        HeuristicSearch<T> h2 = config.createHeuristicSearch(transitData, r2);
        runInParallel(r1, h1, h2);
        DebugHeuristics.debug(alias(r1), h1.heuristics(), alias(r2), h2.heuristics(), r1);
    }

    public void shutdown() {
        config.shutdown();
    }

    /* private methods */

    private Collection<Path<T>> routeUsingStdWorker(TransitDataProvider<T> transitData, RaptorRequest<T> request) {
        return config.createStdWorker(transitData, request).route();
    }

    private Collection<Path<T>> routeUsingMcWorker(TransitDataProvider<T> transitData, RaptorRequest<T> request) {
        HeuristicSearch<T> fwdHeur;
        HeuristicSearch<T> revHeur;
        Heuristics destinationArrivalHeuristicsCheck = null;
        RaptorRequest<T> mcRequest = request;
        final int nAdditionalTransfers = request.searchParams().numberOfAdditionalTransfers();

        if (request.optimizationEnabled(TRANSFERS_STOP_FILTER)) {
            fwdHeur = config.createHeuristicSearch(transitData, NO_WAIT_BEST_TIME, request, FORWARD);
            revHeur = config.createHeuristicSearch(transitData, NO_WAIT_BEST_TIME, request, REVERSE);

            runInParallel(request, revHeur, fwdHeur);

            mcRequest = addStopFilterToRequest(mcRequest, fwdHeur.stopFilter(revHeur, nAdditionalTransfers));

            if (request.optimizationEnabled(PARETO_CHECK_AGAINST_DESTINATION)) {
                destinationArrivalHeuristicsCheck = revHeur.heuristics();
            }
            debugHeuristicResut(request, fwdHeur, revHeur);

        } else if (request.optimizationEnabled(PARETO_CHECK_AGAINST_DESTINATION)) {
            revHeur = config.createHeuristicSearch(transitData, NO_WAIT_BEST_TIME, request, REVERSE);
            revHeur.route();
            destinationArrivalHeuristicsCheck = revHeur.heuristics();
        }
        return config.createMcWorker(transitData, mcRequest, destinationArrivalHeuristicsCheck).route();
    }

    private RaptorRequest<T> addStopFilterToRequest(RaptorRequest<T> request, BitSet stopFilter) {
        RaptorRequestBuilder<T> reqBuilder = request.mutate();
        reqBuilder.searchParams().stopFilter(stopFilter);
        return reqBuilder.build();
    }

    private void debugHeuristicResut(RaptorRequest<?> req, HeuristicSearch<T> fwdHeur, HeuristicSearch<T> revHeur) {
        DebugHeuristics.debug("Forward", fwdHeur.heuristics(), "Reverse", revHeur.heuristics(), req);
    }

    private void runInParallel(RaptorRequest<T> request, Worker<?> w1, Worker<?> w2) {
        if (!runInParallel(request)) {
            w1.route();
            w2.route();
            return;
        }
        try {
            Future<?> f = config.threadPool().submit(w2::route);
            w1.route();
            f.get();
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private String alias(RaptorRequest<?> request) {
        return RequestAlias.alias(request, isMultiThreaded());
    }

    private boolean runInParallel(RaptorRequest<?> request) {
        return isMultiThreaded() && request.optimizationEnabled(PARALLEL);
    }

    private boolean isMultiThreaded() {
        return config.isMultiThreaded();
    }
}
