package org.opentripplanner.transit.raptor;

import java.util.stream.Collectors;
import org.opentripplanner.transit.raptor.api.path.Path;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.response.RaptorResponse;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransitDataProvider;
import org.opentripplanner.transit.raptor.api.transit.RaptorTripSchedule;
import org.opentripplanner.transit.raptor.rangeraptor.configure.RaptorConfig;
import org.opentripplanner.transit.raptor.service.HeuristicSearchTask;
import org.opentripplanner.transit.raptor.service.RangeRaptorDynamicSearch;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * A service for performing Range Raptor routing request.
 *
 * @param <T> The TripSchedule type defined by the user of the range raptor API.
 */
public class RaptorService<T extends RaptorTripSchedule> {

    private static final Logger LOG = LoggerFactory.getLogger(RaptorService.class);

    private final RaptorConfig<T> config;

    public RaptorService(RaptorConfig<T> config) {
        this.config = config;
    }

    public RaptorResponse<T> route(RaptorRequest<T> request, RaptorTransitDataProvider<T> transitData) {
        LOG.debug("Original request: {}", request);
        RaptorResponse<T> response;

        if(request.isDynamicSearch()) {
            response = new RangeRaptorDynamicSearch<>(config, transitData, request).route();
        }
        else {
            response = routeUsingStdWorker(transitData, request);
        }
        if(LOG.isDebugEnabled()) {
            var pathsAsText = response.paths().stream()
                    .map(p -> "\t\n" + p.toString(transitData.stopNameResolver()))
                    .collect(Collectors.joining());
            LOG.debug("Result: {}", pathsAsText);
        }
        return response;
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
        LOG.debug("Run query: {}", request);
        Collection<Path<T>> paths = config.createStdWorker(transitData, request).route();
        return new RaptorResponse<>(paths, request, request);
    }
}
