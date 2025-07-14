package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.alert.siri.mapping.SiriTransportModeMapper.mapTransitMainMode;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.CANNOT_RESOLVE_AGENCY;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.UNKNOWN_STOP;
import static org.opentripplanner.updater.trip.siri.support.NaturalLanguageStringHelper.getFirstStringFromList;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import javax.annotation.Nullable;
import org.opentripplanner.framework.i18n.NonLocalizedString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.basic.TransitMode;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.Route;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.organization.Agency;
import org.opentripplanner.transit.model.organization.Operator;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripOnServiceDate;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.rutebanken.netex.model.BusSubmodeEnumeration;
import org.rutebanken.netex.model.RailSubmodeEnumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedVehicleJourney;
import uk.org.siri.siri21.OccupancyEnumeration;
import uk.org.siri.siri21.VehicleJourneyRef;
import uk.org.siri.siri21.VehicleModesEnumeration;

class AddedTripBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(AddedTripBuilder.class);
  private final TransitEditorService transitService;
  private final EntityResolver entityResolver;
  private final ZoneId timeZone;
  private final Function<Trip, FeedScopedId> getTripPatternId;
  private final FeedScopedId tripId;
  private final Operator operator;
  private final String dataSource;
  private final String lineRef;
  private final Route replacedRoute;
  private final LocalDate serviceDate;
  private final TransitMode transitMode;
  private final String transitSubMode;
  private final List<CallWrapper> calls;
  private final boolean isJourneyPredictionInaccurate;
  private final OccupancyEnumeration occupancy;
  private final boolean cancellation;
  private final String shortName;
  private final String headsign;
  private final List<TripOnServiceDate> replacedTrips;
  private final StopTimesMapper stopTimesMapper;

  AddedTripBuilder(
    EstimatedVehicleJourney estimatedVehicleJourney,
    TransitEditorService transitService,
    EntityResolver entityResolver,
    Function<Trip, FeedScopedId> getTripPatternId
  ) {
    // Verifying values required in SIRI Profile
    // Added ServiceJourneyId
    String newServiceJourneyRef = estimatedVehicleJourney.getEstimatedVehicleJourneyCode();
    Objects.requireNonNull(newServiceJourneyRef, "EstimatedVehicleJourneyCode is required");
    tripId = entityResolver.resolveId(newServiceJourneyRef);

    // OperatorRef of added trip
    Objects.requireNonNull(estimatedVehicleJourney.getOperatorRef(), "OperatorRef is required");
    String operatorRef = estimatedVehicleJourney.getOperatorRef().getValue();
    operator = entityResolver.resolveOperator(operatorRef);

    // DataSource of added trip
    dataSource = estimatedVehicleJourney.getDataSource();

    // LineRef of added trip
    Objects.requireNonNull(estimatedVehicleJourney.getLineRef(), "LineRef is required");
    lineRef = estimatedVehicleJourney.getLineRef().getValue();

    String externalLineRef = estimatedVehicleJourney.getExternalLineRef() != null
      ? estimatedVehicleJourney.getExternalLineRef().getValue()
      : lineRef;
    replacedRoute = entityResolver.resolveRoute(externalLineRef);

    serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);

    shortName = getFirstStringFromList(estimatedVehicleJourney.getPublishedLineNames());

    List<VehicleModesEnumeration> vehicleModes = estimatedVehicleJourney.getVehicleModes();
    transitMode = mapTransitMainMode(vehicleModes);
    transitSubMode = resolveTransitSubMode(transitMode, replacedRoute);

    isJourneyPredictionInaccurate = TRUE.equals(estimatedVehicleJourney.isPredictionInaccurate());
    occupancy = estimatedVehicleJourney.getOccupancy();
    cancellation = TRUE.equals(estimatedVehicleJourney.isCancellation());
    headsign = getFirstStringFromList(estimatedVehicleJourney.getDestinationNames());

    calls = CallWrapper.of(estimatedVehicleJourney);

    this.transitService = transitService;
    this.entityResolver = entityResolver;
    this.getTripPatternId = getTripPatternId;
    timeZone = transitService.getTimeZone();

    replacedTrips = getReplacedVehicleJourneys(estimatedVehicleJourney);
    stopTimesMapper = new StopTimesMapper(entityResolver, timeZone);
  }

  AddedTripBuilder(
    TransitEditorService transitService,
    EntityResolver entityResolver,
    Function<Trip, FeedScopedId> getTripPatternId,
    FeedScopedId tripId,
    Operator operator,
    String lineRef,
    Route replacedRoute,
    LocalDate serviceDate,
    TransitMode transitMode,
    String transitSubMode,
    List<CallWrapper> calls,
    boolean isJourneyPredictionInaccurate,
    OccupancyEnumeration occupancy,
    boolean cancellation,
    String shortName,
    String headsign,
    List<TripOnServiceDate> replacedTrips,
    String dataSource
  ) {
    this.transitService = transitService;
    this.entityResolver = entityResolver;
    this.timeZone = transitService.getTimeZone();
    this.getTripPatternId = getTripPatternId;
    this.tripId = tripId;
    this.operator = operator;
    this.lineRef = lineRef;
    this.replacedRoute = replacedRoute;
    this.serviceDate = serviceDate;
    this.transitMode = transitMode;
    this.transitSubMode = transitSubMode;
    this.calls = calls;
    this.isJourneyPredictionInaccurate = isJourneyPredictionInaccurate;
    this.occupancy = occupancy;
    this.cancellation = cancellation;
    this.shortName = shortName;
    this.headsign = headsign;
    this.replacedTrips = replacedTrips;
    this.dataSource = dataSource;
    stopTimesMapper = new StopTimesMapper(entityResolver, timeZone);
  }

  Result<TripUpdate, UpdateError> build() {
    if (calls.size() < 2) {
      return UpdateError.result(tripId, TOO_FEW_STOPS, dataSource);
    }

    if (serviceDate == null) {
      return UpdateError.result(tripId, NO_START_DATE, dataSource);
    }

    FeedScopedId calServiceId = transitService.getOrCreateServiceIdForDate(serviceDate);
    if (calServiceId == null) {
      return UpdateError.result(tripId, NO_START_DATE, dataSource);
    }

    boolean isAddedRoute = false;
    Route route = entityResolver.resolveRoute(lineRef);
    if (route == null) {
      Agency agency = resolveAgency();
      if (agency == null) {
        return UpdateError.result(tripId, CANNOT_RESOLVE_AGENCY, dataSource);
      }
      route = createRoute(agency);
      isAddedRoute = true;
      LOG.info("Adding route {} to timetableRepository.", route);
    }

    Trip trip = createTrip(route, calServiceId);

    ZonedDateTime departureDate = serviceDate.atStartOfDay(timeZone);

    // Create the "scheduled version" of the trip
    var aimedStopTimes = new ArrayList<StopTime>();
    for (int stopSequence = 0; stopSequence < calls.size(); stopSequence++) {
      StopTime stopTime = stopTimesMapper.createAimedStopTime(
        trip,
        departureDate,
        stopSequence,
        calls.get(stopSequence),
        stopSequence == 0,
        stopSequence == (calls.size() - 1)
      );

      // Drop this update if the call refers to an unknown stop (not present in the site repository).
      if (stopTime == null) {
        return UpdateError.result(tripId, UNKNOWN_STOP, dataSource);
      }

      aimedStopTimes.add(stopTime);
    }

    // TODO: We always create a new TripPattern to be able to modify its scheduled timetable
    StopPattern stopPattern = new StopPattern(aimedStopTimes);

    // validate the scheduled trip times
    // they are in general superseded by real-time trip times
    // but in case of trip cancellation, OTP will fall back to scheduled trip times
    // therefore they must be valid
    var tripTimes = TripTimesFactory.tripTimes(
      trip,
      aimedStopTimes,
      transitService.getDeduplicator()
    ).withServiceCode(transitService.getServiceCode(trip.getServiceId()));
    tripTimes.validateNonIncreasingTimes();

    TripPattern pattern = TripPattern.of(getTripPatternId.apply(trip))
      .withRoute(trip.getRoute())
      .withMode(trip.getMode())
      .withNetexSubmode(trip.getNetexSubMode())
      .withStopPattern(stopPattern)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .build();

    RealTimeTripTimesBuilder builder = tripTimes.createRealTimeFromScheduledTimes();

    // Loop through calls again and apply updates
    for (int stopSequence = 0; stopSequence < calls.size(); stopSequence++) {
      TimetableHelper.applyUpdates(
        departureDate,
        builder,
        stopSequence,
        stopSequence == (calls.size() - 1),
        isJourneyPredictionInaccurate,
        calls.get(stopSequence),
        occupancy
      );
    }

    if (cancellation || stopPattern.isAllStopsNonRoutable()) {
      builder.cancelTrip();
    } else {
      builder.withRealTimeState(RealTimeState.ADDED);
    }

    /* Validate */
    var tripOnServiceDate = TripOnServiceDate.of(tripId)
      .withTrip(trip)
      .withServiceDate(serviceDate)
      .withReplacementFor(replacedTrips)
      .build();

    try {
      return Result.success(
        new TripUpdate(
          stopPattern,
          builder.build(),
          serviceDate,
          tripOnServiceDate,
          pattern,
          isAddedRoute,
          dataSource
        )
      );
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e, dataSource);
    }
  }

  /**
   * Method to create a Route. Commonly used to create a route if a real-time message
   * refers to a route that is not in the transit model.
   * If no name is given for the route, an empty string will be set as the name.
   *
   * @return a new Route
   */
  private Route createRoute(Agency agency) {
    var routeBuilder = Route.of(entityResolver.resolveId(lineRef));

    routeBuilder.withShortName(shortName);
    routeBuilder.withMode(transitMode);
    routeBuilder.withNetexSubmode(transitSubMode);
    routeBuilder.withOperator(operator);
    routeBuilder.withAgency(agency);

    return routeBuilder.build();
  }

  /**
   * Attempt to find the agency to which this new trip belongs.
   * The algorithm retrieves any route operated by the same operator as the one operating this new
   * trip and resolves its agency.
   * If no route with the same operator can be found, the algorithm falls back to retrieving the
   * agency operating the replaced route.
   * If none can be found the method returns null.
   */
  @Nullable
  private Agency resolveAgency() {
    return transitService
      .listRoutes()
      .stream()
      .filter(r -> r != null && r.getOperator() != null && r.getOperator().equals(operator))
      .findFirst()
      .map(Route::getAgency)
      .orElseGet(() -> replacedRoute != null ? replacedRoute.getAgency() : null);
  }

  private Trip createTrip(Route route, FeedScopedId calServiceId) {
    var tripBuilder = Trip.of(tripId);
    tripBuilder.withRoute(route);

    // Explicitly set TransitMode on Trip - in case it differs from Route
    tripBuilder.withMode(transitMode);
    tripBuilder.withNetexSubmode(transitSubMode);

    tripBuilder.withServiceId(calServiceId);

    // Use destinationName as default headsign - if provided
    tripBuilder.withHeadsign(NonLocalizedString.ofNullable(headsign));

    tripBuilder.withOperator(operator);

    return tripBuilder.build();
  }

  /**
   * Resolves submode based on added trip's mode and replacedRoute's mode
   *
   * @param transitMode   Mode of the added trip
   * @param replacedRoute Route that is being replaced
   * @return String-representation of submode
   */
  static String resolveTransitSubMode(TransitMode transitMode, Route replacedRoute) {
    if (replacedRoute == null) {
      return null;
    }
    TransitMode replacedRouteMode = replacedRoute.getMode();

    if (replacedRouteMode != TransitMode.RAIL) {
      return null;
    }

    return switch (transitMode) {
      // Replacement-route is also RAIL
      case RAIL -> RailSubmodeEnumeration.REPLACEMENT_RAIL_SERVICE.value();
      // Replacement-route is BUS
      case BUS -> BusSubmodeEnumeration.RAIL_REPLACEMENT_BUS.value();
      default -> null;
    };
  }

  private List<TripOnServiceDate> getReplacedVehicleJourneys(
    EstimatedVehicleJourney estimatedVehicleJourney
  ) {
    List<TripOnServiceDate> listOfReplacedVehicleJourneys = new ArrayList<>();

    // VehicleJourneyRef is the reference to the serviceJourney being replaced.
    VehicleJourneyRef vehicleJourneyRef = estimatedVehicleJourney.getVehicleJourneyRef(); // getVehicleJourneyRef
    if (vehicleJourneyRef != null) {
      var replacedDatedServiceJourney = entityResolver.resolveTripOnServiceDate(
        vehicleJourneyRef.getValue()
      );
      if (replacedDatedServiceJourney != null) {
        listOfReplacedVehicleJourneys.add(replacedDatedServiceJourney);
      }
    }

    // Add additional replaced service journeys if present.
    estimatedVehicleJourney
      .getAdditionalVehicleJourneyReves()
      .stream()
      .map(entityResolver::resolveTripOnServiceDate)
      .filter(Objects::nonNull)
      .forEach(listOfReplacedVehicleJourneys::add);

    return listOfReplacedVehicleJourneys;
  }
}
