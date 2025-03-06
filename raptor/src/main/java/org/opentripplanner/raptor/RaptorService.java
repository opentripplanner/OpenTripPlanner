package org.opentripplanner.raptor;

import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.response.RaptorResponse;
import org.opentripplanner.raptor.configure.RaptorConfig;
import org.opentripplanner.raptor.service.DefaultStopArrivals;
import org.opentripplanner.raptor.service.HeuristicSearchTask;
import org.opentripplanner.raptor.service.RangeRaptorDynamicSearch;
import org.opentripplanner.raptor.spi.ExtraMcRouterSearch;
import org.opentripplanner.raptor.spi.RaptorTransitDataProvider;
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

  @Nullable
  private final ExtraMcRouterSearch<T> extraMcSearch;

  public RaptorService(RaptorConfig<T> config, @Nullable ExtraMcRouterSearch<T> extraMcSearch) {
    this.config = config;
    this.extraMcSearch = extraMcSearch;
  }

  public RaptorService(RaptorConfig<T> config) {
    this(config, null);
  }

  public RaptorResponse<T> route(
    RaptorRequest<T> request,
    RaptorTransitDataProvider<T> transitData
  ) {
    logRequest(request);
    RaptorResponse<T> response;

    if (request.isDynamicSearch()) {
      response = new RangeRaptorDynamicSearch<>(
        config,
        transitData,
        extraMcSearch,
        request
      ).route();
    } else {
      response = routeUsingStdWorker(transitData, request);
    }
    logResponse(transitData, response);
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
    var rangeRaptorRouter = config.createRangeRaptorWithStdWorker(transitData, request);
    var result = rangeRaptorRouter.route();
    var arrivals = new DefaultStopArrivals(result);
    return new RaptorResponse<>(result.extractPaths(), arrivals, request, false);
  }

  private static <T extends RaptorTripSchedule> void logRequest(RaptorRequest<T> request) {
    LOG.debug("Original request: {}", request);
  }

  private static <T extends RaptorTripSchedule> void logResponse(
    RaptorTransitDataProvider<T> transitData,
    RaptorResponse<T> response
  ) {
    if (LOG.isDebugEnabled()) {
      var pathsAsText = response
        .paths()
        .stream()
        .map(p -> "\t\n" + p.toString(transitData.stopNameResolver()))
        .collect(Collectors.joining());
      LOG.debug("Result: {}", pathsAsText);
    }
  }
}
