package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import static org.opentripplanner.raptor.api.request.Optimization.PARALLEL;

import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import javax.annotation.Nullable;
import org.opentripplanner.framework.application.OTPFeature;
import org.opentripplanner.raptor.api.model.GeneralizedCostRelaxFunction;
import org.opentripplanner.raptor.api.model.RaptorAccessEgress;
import org.opentripplanner.raptor.api.model.RaptorConstants;
import org.opentripplanner.raptor.api.model.RaptorCostConverter;
import org.opentripplanner.raptor.api.model.RaptorTripSchedule;
import org.opentripplanner.raptor.api.model.RelaxFunction;
import org.opentripplanner.raptor.api.request.DebugRequestBuilder;
import org.opentripplanner.raptor.api.request.MultiCriteriaRequest;
import org.opentripplanner.raptor.api.request.Optimization;
import org.opentripplanner.raptor.api.request.RaptorRequest;
import org.opentripplanner.raptor.api.request.RaptorRequestBuilder;
import org.opentripplanner.raptor.api.request.RaptorViaLocation;
import org.opentripplanner.raptor.rangeraptor.SystemErrDebugLogger;
import org.opentripplanner.routing.algorithm.raptoradapter.router.performance.PerformanceTimersForRaptor;
import org.opentripplanner.routing.api.request.DebugEventType;
import org.opentripplanner.routing.api.request.RouteRequest;
import org.opentripplanner.routing.api.request.framework.CostLinearFunction;
import org.opentripplanner.routing.api.request.preference.TransitPreferences;
import org.opentripplanner.routing.api.request.via.ViaLocation;
import org.opentripplanner.routing.via.ViaCoordinateTransferFactory;
import org.opentripplanner.transit.model.network.grouppriority.DefaultTransitGroupPriorityCalculator;

public class RaptorRequestMapper<T extends RaptorTripSchedule> {

  private final RouteRequest request;
  private final Collection<? extends RaptorAccessEgress> accessPaths;
  private final Collection<? extends RaptorAccessEgress> egressPaths;
  private final long transitSearchTimeZeroEpocSecond;
  private final boolean isMultiThreadedEnbled;
  private final MeterRegistry meterRegistry;
  private final ViaCoordinateTransferFactory viaTransferResolver;
  private final LookupStopIndexCallback lookUpStopIndex;

  private RaptorRequestMapper(
    RouteRequest request,
    boolean isMultiThreaded,
    Collection<? extends RaptorAccessEgress> accessPaths,
    Collection<? extends RaptorAccessEgress> egressPaths,
    long transitSearchTimeZeroEpocSecond,
    @Nullable MeterRegistry meterRegistry,
    ViaCoordinateTransferFactory viaTransferResolver,
    LookupStopIndexCallback lookUpStopIndex
  ) {
    this.request = Objects.requireNonNull(request);
    this.isMultiThreadedEnbled = isMultiThreaded;
    this.accessPaths = Objects.requireNonNull(accessPaths);
    this.egressPaths = Objects.requireNonNull(egressPaths);
    this.transitSearchTimeZeroEpocSecond = transitSearchTimeZeroEpocSecond;
    this.meterRegistry = meterRegistry;
    this.viaTransferResolver = Objects.requireNonNull(viaTransferResolver);
    this.lookUpStopIndex = Objects.requireNonNull(lookUpStopIndex);
  }

  public static <T extends RaptorTripSchedule> RaptorRequest<T> mapRequest(
    RouteRequest request,
    ZonedDateTime transitSearchTimeZero,
    boolean isMultiThreaded,
    Collection<? extends RaptorAccessEgress> accessPaths,
    Collection<? extends RaptorAccessEgress> egressPaths,
    MeterRegistry meterRegistry,
    ViaCoordinateTransferFactory viaTransferResolver,
    LookupStopIndexCallback lookUpStopIndex
  ) {
    return new RaptorRequestMapper<T>(
      request,
      isMultiThreaded,
      accessPaths,
      egressPaths,
      transitSearchTimeZero.toEpochSecond(),
      meterRegistry,
      viaTransferResolver,
      lookUpStopIndex
    ).doMap();
  }

  private RaptorRequest<T> doMap() {
    var builder = new RaptorRequestBuilder<T>();
    var searchParams = builder.searchParams();
    var preferences = request.preferences();

    // TODO Fix the Raptor search so pass-through and via search can be used together.
    if (hasViaLocationsAndPassThroughLocations()) {
      throw new IllegalArgumentException(
        "A mix of via-locations and pass-through is not allowed in this version."
      );
    }

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

    builder.searchParams().addViaLocations(mapViaLocations());

    if (preferences.transfer().maxTransfers() != null) {
      searchParams.maxNumberOfTransfers(preferences.transfer().maxTransfers());
    }

    if (preferences.transfer().maxAdditionalTransfers() != null) {
      searchParams.numberOfAdditionalTransfers(preferences.transfer().maxAdditionalTransfers());
    }

    builder.withMultiCriteria(mcBuilder -> {
      var pt = preferences.transit();
      var r = pt.raptor();

      // relax transit group priority can be used with via-visit-stop, but not with pass-through
      if (pt.isRelaxTransitGroupPrioritySet() && !hasPassThroughOnly()) {
        mapRelaxTransitGroupPriority(mcBuilder, pt);
      } else if (!request.isViaSearch()) {
        // The deprecated relaxGeneralizedCostAtDestination is only enabled, if there is no
        // via location and the relaxTransitGroupPriority is not used (Normal).
        r.relaxGeneralizedCostAtDestination().ifPresent(mcBuilder::withRelaxCostAtDestination);
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

  private boolean hasPassThroughOnly() {
    return (
      request.isViaSearch() &&
      request.getViaLocations().stream().allMatch(ViaLocation::isPassThroughLocation)
    );
  }

  private boolean hasViaLocationsOnly() {
    return (
      request.isViaSearch() &&
      request.getViaLocations().stream().noneMatch(ViaLocation::isPassThroughLocation)
    );
  }

  private boolean hasViaLocationsAndPassThroughLocations() {
    var c = request.getViaLocations();
    return (
      request.isViaSearch() &&
      c.stream().anyMatch(ViaLocation::isPassThroughLocation) &&
      c.stream().anyMatch(Predicate.not(ViaLocation::isPassThroughLocation))
    );
  }

  private List<RaptorViaLocation> mapViaLocations() {
    return request.getViaLocations().stream().map(this::mapViaLocation).toList();
  }

  private RaptorViaLocation mapViaLocation(ViaLocation input) {
    if (input.isPassThroughLocation()) {
      var builder = RaptorViaLocation.passThrough(input.label());
      for (int stopIndex : lookUpStopIndex.lookupStopLocationIndexes(input.stopLocationIds())) {
        builder.addPassThroughStop(stopIndex);
      }
      return builder.build();
    }
    // Visit Via location
    else {
      var viaStops = new HashSet<Integer>();
      var builder = RaptorViaLocation.via(input.label(), input.minimumWaitTime());
      for (int stopIndex : lookUpStopIndex.lookupStopLocationIndexes(input.stopLocationIds())) {
        builder.addViaStop(stopIndex);
        viaStops.add(stopIndex);
      }
      for (var coordinate : input.coordinates()) {
        var viaTransfers = viaTransferResolver.createViaTransfers(
          request,
          input.label(),
          coordinate
        );
        for (var it : viaTransfers) {
          // If via-stop and via-transfers are used together then walking from a stop
          // to the coordinate and back is not pareto optimal, using just the stop
          // is the optimal option.
          if (it.stop() == it.fromStopIndex() && viaStops.contains(it.stop())) {
            continue;
          }
          builder.addViaTransfer(it.fromStopIndex(), it);
        }
      }
      return builder.build();
    }
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

  private static void mapRelaxTransitGroupPriority(
    MultiCriteriaRequest.Builder<?> mcBuilder,
    TransitPreferences pt
  ) {
    mcBuilder.withTransitPriorityCalculator(new DefaultTransitGroupPriorityCalculator());
    mcBuilder.withRelaxC1(mapRelaxCost(pt.relaxTransitGroupPriority()));
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
