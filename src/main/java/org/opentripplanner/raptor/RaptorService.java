package org.opentripplanner.raptor;

import java.util.stream.Collectors;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.rangeraptor.internalapi.Worker;
import org.opentripplanner.raptor.service.HeuristicSearchTask;
import org.opentripplanner.raptor.service.RangeRaptorDynamicSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
import org.opentripplanner.raptor.spi.RaptorTripSchedule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  public RaptorResponse<T> route(
    RaptorRequest<T> request,
    RaptorTransitDataProvider<T> transitData
  ) {
    LOG.debug("Original request: {}", request);
    RaptorResponse<T> response;

    if (request.isDynamicSearch()) {
      response = new RangeRaptorDynamicSearch<>(config, transitData, request).route();
    } else {
      response = routeUsingStdWorker(transitData, request);
    }
    if (LOG.isDebugEnabled()) {
      var pathsAsText = response
        .paths()
        .stream()
        .map(p -> "\t\n" + p.toString(transitData.stopNameResolver()))
        .collect(Collectors.joining());
      LOG.debug("Result: {}", pathsAsText);
    }
    return response;
  }

  /**
   * TODO Add back the possibility to compare heuristics using a test - like the SpeedTest,
   *      but maybe better to make a separate test.
   */
  @SuppressWarnings("unused")
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

  /* private methods */

  private RaptorResponse<T> routeUsingStdWorker(
    RaptorTransitDataProvider<T> transitData,
    RaptorRequest<T> request
  ) {
    Worker<T> worker = config.createStdWorker(transitData, request);
    worker.route();
    return new RaptorResponse<>(worker.paths(), worker.stopArrivals(), request, request);
  }
}
