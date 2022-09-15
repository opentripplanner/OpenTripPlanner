package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.opentripplanner.transit.raptor.api.request.Optimization.PARALLEL;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import org.opentripplanner.routing.algorithm.raptoradapter.router.performance.PerformanceTimersForRaptor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.SlackProvider;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripSchedule;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.transit.raptor.api.request.Optimization;
import org.opentripplanner.transit.raptor.api.request.RaptorProfile;
import org.opentripplanner.transit.raptor.api.request.RaptorRequest;
import org.opentripplanner.transit.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.transit.raptor.api.request.SearchParams;
import org.opentripplanner.transit.raptor.api.transit.RaptorTransfer;
import org.opentripplanner.transit.raptor.rangeraptor.SystemErrDebugLogger;
import org.opentripplanner.util.OTPFeature;

public class RaptorRequestMapper {

  private final RouteRequest request;
  private final Collection<? extends RaptorTransfer> accessPaths;
  private final Collection<? extends RaptorTransfer> egressPaths;
  private final long transitSearchTimeZeroEpocSecond;
  private final boolean isMultiThreadedEnbled;
  private final MeterRegistry meterRegistry;

  private RaptorRequestMapper(
    RouteRequest request,
    boolean isMultiThreaded,
    Collection<? extends RaptorTransfer> accessPaths,
    Collection<? extends RaptorTransfer> egressPaths,
    long transitSearchTimeZeroEpocSecond,
    MeterRegistry meterRegistry
  ) {
    this.request = request;
    this.isMultiThreadedEnbled = isMultiThreaded;
    this.accessPaths = accessPaths;
    this.egressPaths = egressPaths;
    this.transitSearchTimeZeroEpocSecond = transitSearchTimeZeroEpocSecond;
    this.meterRegistry = meterRegistry;
  }

  public static RaptorRequest<TripSchedule> mapRequest(
    RouteRequest request,
    ZonedDateTime transitSearchTimeZero,
    boolean isMultiThreaded,
    Collection<? extends RaptorTransfer> accessPaths,
    Collection<? extends RaptorTransfer> egressPaths,
    MeterRegistry meterRegistry
  ) {
    return new RaptorRequestMapper(
      request,
      isMultiThreaded,
      accessPaths,
      egressPaths,
      transitSearchTimeZero.toEpochSecond(),
      meterRegistry
    )
      .doMap();
  }

  private RaptorRequest<TripSchedule> doMap() {
    var builder = new RaptorRequestBuilder<TripSchedule>();
    var searchParams = builder.searchParams();

    var preferences = request.preferences();

    if (request.pageCursor() == null) {
      int time = relativeTime(request.dateTime());

      int timeLimit = relativeTime(preferences.transit().raptorOptions().getTimeLimit());

      if (request.arriveBy()) {
        searchParams.latestArrivalTime(time);
        searchParams.earliestDepartureTime(timeLimit);
      } else {
        searchParams.earliestDepartureTime(time);
        searchParams.latestArrivalTime(timeLimit);
      }
      searchParams.searchWindow(request.searchWindow());
    } else {
      var c = request.pageCursor();

      if (c.earliestDepartureTime != null) {
        searchParams.earliestDepartureTime(relativeTime(c.earliestDepartureTime));
      }
      if (c.latestArrivalTime != null) {
        searchParams.latestArrivalTime(relativeTime(c.latestArrivalTime));
      }
      searchParams.searchWindow(c.searchWindow);
    }

    if (preferences.transfer().maxTransfers() != null) {
      searchParams.maxNumberOfTransfers(preferences.transfer().maxTransfers());
    }

    for (Optimization optimization : preferences.transit().raptorOptions().getOptimizations()) {
      if (optimization.is(PARALLEL)) {
        if (isMultiThreadedEnbled) {
          builder.enableOptimization(optimization);
        }
      } else {
        builder.enableOptimization(optimization);
      }
    }

    builder.profile(preferences.transit().raptorOptions().getProfile());
    builder.searchDirection(preferences.transit().raptorOptions().getSearchDirection());

    builder
      .profile(RaptorProfile.MULTI_CRITERIA)
      .enableOptimization(Optimization.PARETO_CHECK_AGAINST_DESTINATION)
      .slackProvider(
        new SlackProvider(
          preferences.transfer().slack(),
          preferences.transit().boardSlack(),
          preferences.transit().alightSlack()
        )
      );

    builder
      .searchParams()
      .timetableEnabled(request.timetableView())
      .constrainedTransfersEnabled(OTPFeature.TransferConstraints.isOn())
      .addAccessPaths(accessPaths)
      .addEgressPaths(egressPaths);

    var raptorDebugging = request.journey().transit().raptorDebugging();

    if (raptorDebugging.isEnabled()) {
      var debug = builder.debug();
      var debugLogger = new SystemErrDebugLogger(true);

      debug
        .addStops(raptorDebugging.stops())
        .setPath(raptorDebugging.path())
        .debugPathFromStopIndex(raptorDebugging.debugPathFromStopIndex())
        .stopArrivalListener(debugLogger::stopArrivalLister)
        .patternRideDebugListener(debugLogger::patternRideLister)
        .pathFilteringListener(debugLogger::pathFilteringListener)
        .logger(debugLogger);
    }

    if (!request.timetableView() && request.arriveBy()) {
      builder.searchParams().preferLateArrival(true);
    }

    // Add this last, it depends on generating an alias from the set values
    builder.performanceTimers(
      new PerformanceTimersForRaptor(
        builder.generateAlias(),
        preferences.system().tags(),
        meterRegistry
      )
    );

    return builder.build();
  }

  private int relativeTime(Instant time) {
    if (time == null) {
      return SearchParams.TIME_NOT_SET;
    }
    return (int) (time.getEpochSecond() - transitSearchTimeZeroEpocSecond);
  }
}
