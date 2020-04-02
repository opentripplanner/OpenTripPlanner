package org.opentripplanner.transit.raptor;

import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.transit.raptor.service.HeuristicSearchTask;
import org.opentripplanner.transit.raptor.service.RangRaptorDynamicSearch;

import java.util.Collection;

/**
 * A service for performing Range Raptor routing request.
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public class RaptorService<T extends RaptorTripSchedule> {

    private final RaptorConfig<T> config;

    public RaptorService(RaptorConfig<T> config) {
        this.config = config;
    }

    public RaptorResponse<T> route(RaptorRequest<T> request, RaptorTransitDataProvider<T> transitData) {
        if(request.isDynamicSearch()) {
            return new RangRaptorDynamicSearch<>(config, transitData, request).route();
        }
        return routeUsingStdWorker(transitData, request);
    }

    public void compareHeuristics(
            RaptorRequest<T> r1,
            RaptorRequest<T> r2,
            RaptorTransitDataProvider<T> transitData
    ) {
        HeuristicSearchTask<T> fwdHeur = new HeuristicSearchTask<>(r1, config, transitData);
        HeuristicSearchTask<T> revHeur = new HeuristicSearchTask<>(r2, config, transitData);

        fwdHeur.forceRun();
        revHeur.forceRun();

        fwdHeur.debugCompareResult(revHeur);
    }

    public void shutdown() {
        config.shutdown();
    }

    /* private methods */

    private RaptorResponse<T> routeUsingStdWorker(RaptorTransitDataProvider<T> transitData, RaptorRequest<T> request) {
        Collection<Path<T>> paths = config.createStdWorker(transitData, request).route();
        return new RaptorResponse<>(paths, request, request);
    }
}
