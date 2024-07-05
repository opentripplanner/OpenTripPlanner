package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.opentripplanner.raptor.api.request.Optimization.PARALLEL;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.List;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.request.DebugRequestBuilder;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.PassThroughPoint;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.rangeraptor.SystemErrDebugLogger;
import org.opentripplanner.routing.algorithm.raptoradapter.router.performance.PerformanceTimersForRaptor;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.RaptorCostConverter;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.cost.grouppriority.TransitGroupPriority32n;
import org.opentripplanner.routing.api.request.DebugEventType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.transit.model.site.StopLocation;

public class RaptorRequestMapper<T extends RaptorTripSchedule> {

  private final RouteRequest request;
  private final Collection<? extends RaptorAccessEgress> accessPaths;
  private final Collection<? extends RaptorAccessEgress> egressPaths;
  private final long transitSearchTimeZeroEpocSecond;
  private final boolean isMultiThreadedEnbled;
  private final MeterRegistry meterRegistry;

  private RaptorRequestMapper(
    RouteRequest request,
    boolean isMultiThreaded,
    Collection<? extends RaptorAccessEgress> accessPaths,
    Collection<? extends RaptorAccessEgress> egressPaths,
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

  public static <T extends RaptorTripSchedule> RaptorRequest<T> mapRequest(
    RouteRequest request,
    ZonedDateTime transitSearchTimeZero,
    boolean isMultiThreaded,
    Collection<? extends RaptorAccessEgress> accessPaths,
    Collection<? extends RaptorAccessEgress> egressPaths,
    MeterRegistry meterRegistry
  ) {
    return new RaptorRequestMapper<T>(
      request,
      isMultiThreaded,
      accessPaths,
      egressPaths,
      transitSearchTimeZero.toEpochSecond(),
      meterRegistry
    )
      .doMap();
  }

  private RaptorRequest<T> doMap() {
    var builder = new RaptorRequestBuilder<T>();
    var searchParams = builder.searchParams();

    var preferences = request.preferences();

    if (request.pageCursor() == null) {
      int time = relativeTime(request.dateTime());

      int timeLimit = relativeTime(preferences.transit().raptor().timeLimit());

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

      if (c.earliestDepartureTime() != null) {
        searchParams.earliestDepartureTime(relativeTime(c.earliestDepartureTime()));
      }
      if (c.latestArrivalTime() != null) {
        searchParams.latestArrivalTime(relativeTime(c.latestArrivalTime()));
      }
      searchParams.searchWindow(c.searchWindow());
    }

    if (preferences.transfer().maxTransfers() != null) {
      searchParams.maxNumberOfTransfers(preferences.transfer().maxTransfers());
    }

    if (preferences.transfer().maxAdditionalTransfers() != null) {
      searchParams.numberOfAdditionalTransfers(preferences.transfer().maxAdditionalTransfers());
    }

    builder.withMultiCriteria(mcBuilder -> {
      var pt = preferences.transit();
      var r = pt.raptor();

      // Note! If a pass-through-point exists, then the transit-group-priority feature is disabled
      if (!request.getPassThroughPoints().isEmpty()) {
        mcBuilder.withPassThroughPoints(mapPassThroughPoints());
        r.relaxGeneralizedCostAtDestination().ifPresent(mcBuilder::withRelaxCostAtDestination);
      } else if (!pt.relaxTransitGroupPriority().isNormal()) {
        mcBuilder.withTransitPriorityCalculator(TransitGroupPriority32n.priorityCalculator());
        mcBuilder.withRelaxC1(mapRelaxCost(pt.relaxTransitGroupPriority()));
      }
    });

    for (Optimization optimization : preferences.transit().raptor().optimizations()) {
      if (optimization.is(PARALLEL)) {
        if (isMultiThreadedEnbled) {
          builder.enableOptimization(optimization);
        }
      } else {
        builder.enableOptimization(optimization);
      }
    }

    builder.profile(preferences.transit().raptor().profile());
    builder.searchDirection(preferences.transit().raptor().searchDirection());

    builder
      .searchParams()
      .timetable(request.timetableView())
      .constrainedTransfers(OTPFeature.TransferConstraints.isOn())
      .addAccessPaths(accessPaths)
      .addEgressPaths(egressPaths);

    var raptorDebugging = request.journey().transit().raptorDebugging();

    if (raptorDebugging.isEnabled()) {
      var debug = builder.debug();
      var debugLogger = new SystemErrDebugLogger(true, false);

      debug
        .addStops(raptorDebugging.stops())
        .setPath(raptorDebugging.path())
        .debugPathFromStopIndex(raptorDebugging.debugPathFromStopIndex())
        .logger(debugLogger);

      for (var type : raptorDebugging.eventTypes()) {
        addLogListenerForEachEventTypeRequested(debug, type, debugLogger);
      }
    }

    if (!request.timetableView() && request.arriveBy()) {
      builder.searchParams().preferLateArrival(true);
    }

    // Add this last, it depends on generating an alias from the set values
    if (meterRegistry != null) {
      builder.performanceTimers(
        new PerformanceTimersForRaptor(
          builder.generateAlias(),
          preferences.system().tags(),
          meterRegistry
        )
      );
    }

    return builder.build();
  }

  private List<PassThroughPoint> mapPassThroughPoints() {
    return request
      .getPassThroughPoints()
      .stream()
      .map(p -> {
        final int[] stops = p.stopLocations().stream().mapToInt(StopLocation::getIndex).toArray();
        return new PassThroughPoint(p.name(), stops);
      })
      .toList();
  }

  static RelaxFunction mapRelaxCost(CostLinearFunction relax) {
    if (relax == null) {
      return null;
    }
    return GeneralizedCostRelaxFunction.of(
      relax.coefficient(),
      RaptorCostConverter.toRaptorCost(relax.constant().toSeconds())
    );
  }

  private int relativeTime(Instant time) {
    if (time == null) {
      return RaptorConstants.TIME_NOT_SET;
    }
    return (int) (time.getEpochSecond() - transitSearchTimeZeroEpocSecond);
  }

  private static void addLogListenerForEachEventTypeRequested(
    DebugRequestBuilder target,
    DebugEventType type,
    SystemErrDebugLogger logger
  ) {
    switch (type) {
      case STOP_ARRIVALS -> target.stopArrivalListener(logger::stopArrivalLister);
      case PATTERN_RIDES -> target.patternRideDebugListener(logger::patternRideLister);
      case DESTINATION_ARRIVALS -> target.pathFilteringListener(logger::pathFilteringListener);
    }
  }
}
