package org.opentripplanner.routing.algorithm;

import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;
import org.opentripplanner.raptor.api.request.RaptorTuningParameters;
import org.opentripplanner.routing.algorithm.raptoradapter.router.AdditionalSearchDays;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripAndServiceDateResolver;
import org.opentripplanner.routing.algorithm.raptoradapter.router.startonboardaccess.TripLocationResolver;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.error.InvalidRoutingInputException;
import org.opentripplanner.transit.service.TransitService;
import org.opentripplanner.utils.time.ServiceDateUtils;

/**
 * Pre-processes a {@link RouteRequest} into a {@link RoutingWorkerRequest} for usage in
 * {@link RoutingWorker}. This includes extending the request with a page cursor and amending
 * its data with additional resolved data, such as additional search days.
 */
public final class RequestPreProcessor {

  private final TransitService transitService;
  private final RaptorTuningParameters tuningParameters;
  private final ZoneId zoneId;

  public RequestPreProcessor(
    TransitService transitService,
    RaptorTuningParameters tuningParameters,
    ZoneId zoneId
  ) {
    this.tuningParameters = tuningParameters;
    this.transitService = transitService;
    this.zoneId = zoneId;
  }

  /**
   * Pre-process {@code originalRequest} and return a {@link RoutingWorkerRequest} with all values
   * needed to initialize a {@link RoutingWorker}.
   */
  public RoutingWorkerRequest computeRequest(RouteRequest originalRequest) {
    var request = originalRequest.withPageCursor();
    if (request.isStartOnBoardAccessRequest()) {
      request = prepareRequestForStartOnBoardAccess(request);
    }
    var transitSearchTimeZero = ServiceDateUtils.asStartOfService(request.dateTime(), zoneId);
    var additionalSearchDays = createAdditionalSearchDays(tuningParameters, zoneId, request);
    return new RoutingWorkerRequest(request, transitSearchTimeZero, additionalSearchDays);
  }

  private static AdditionalSearchDays createAdditionalSearchDays(
    RaptorTuningParameters raptorTuningParameters,
    ZoneId zoneId,
    RouteRequest request
  ) {
    var dateTime = Objects.requireNonNull(request.dateTime());
    var searchDateTime = ZonedDateTime.ofInstant(dateTime, zoneId);
    var maxWindow = raptorTuningParameters.dynamicSearchWindowCoefficients().maxWindow();

    return new AdditionalSearchDays(
      request.arriveBy(),
      searchDateTime,
      request.searchWindow(),
      maxWindow,
      request.preferences().system().maxJourneyDuration()
    );
  }

  /**
   * This prepares the request for a start-on-board raptor search by setting its dateTime to the
   * resolved boarding time and the search window to the given iteration step duration. The boarding
   * time is resolved based on timetable data from {@link TransitService} (see
   * {@link TripLocationResolver#resolve}). This produces exactly one Raptor
   * iteration at the boarding time while keeping a valid search window for the filter chain.
   */
  private RouteRequest prepareRequestForStartOnBoardAccess(RouteRequest request) {
    var fromLocation = request.from();
    var tripLocation = fromLocation != null ? fromLocation.tripLocation() : null;
    if (tripLocation == null) {
      throw new InvalidRoutingInputException(
        "On-board access request has no trip location in 'from'"
      );
    }

    var tripAndServiceDate = new TripAndServiceDateResolver(transitService).resolve(
      tripLocation.tripOnDateReference()
    );
    var aimedDeparture = tripLocation.aimedDepartureTime();
    Integer aimedDepartureSeconds = aimedDeparture == null
      ? null
      : ServiceDateUtils.secondsSinceStartOfTime(
          ServiceDateUtils.asStartOfService(tripAndServiceDate.serviceDate(), zoneId),
          aimedDeparture
        );
    var locationInTripPattern = new TripLocationResolver(transitService).resolve(
      tripAndServiceDate,
      tripLocation.stopLocationId(),
      aimedDepartureSeconds
    );
    var boardingDateTime = ServiceDateUtils.toZonedDateTime(
      tripAndServiceDate.serviceDate(),
      zoneId,
      locationInTripPattern.boardingTimeSeconds()
    ).toInstant();
    var iterationStep = Duration.ofSeconds(tuningParameters.iterationDepartureStepInSeconds());

    var requestBuilder = request.copyOf();

    // The datetime must be set so that the raptor data gets data corresponding to the resolved
    // boarding time
    requestBuilder.withDateTime(boardingDateTime);

    // The iteration step must be set to the time of a single iteration. A zero-duration window does
    // not work, as the results would then be filtered out by the filter chain, since it filters
    // out results that don't fall within the window.
    requestBuilder.withSearchWindow(iterationStep);

    return requestBuilder.buildRequest();
  }
}
