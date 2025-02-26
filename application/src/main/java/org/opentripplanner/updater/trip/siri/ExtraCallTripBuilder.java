package org.opentripplanner.updater.trip.siri;

import static java.lang.Boolean.TRUE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.NO_VALID_STOPS;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.StopPattern;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeState;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimesFactory;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri20.EstimatedVehicleJourney;
import uk.org.siri.siri20.OccupancyEnumeration;

class ExtraCallTripBuilder {

  private static final Logger LOG = LoggerFactory.getLogger(ExtraCallTripBuilder.class);
  private final TransitEditorService transitService;
  private final ZoneId timeZone;
  private final Function<Trip, FeedScopedId> getTripPatternId;
  private final Trip trip;
  private final String dataSource;
  private final LocalDate serviceDate;
  private final List<CallWrapper> calls;
  private final boolean isJourneyPredictionInaccurate;
  private final OccupancyEnumeration occupancy;
  private final boolean cancellation;
  private final StopTimesMapper stopTimesMapper;

  ExtraCallTripBuilder(
    EstimatedVehicleJourney estimatedVehicleJourney,
    TransitEditorService transitService,
    EntityResolver entityResolver,
    Function<Trip, FeedScopedId> getTripPatternId,
    Trip trip
  ) {
    this.trip = Objects.requireNonNull(trip);

    // DataSource of added trip
    dataSource = estimatedVehicleJourney.getDataSource();

    serviceDate = entityResolver.resolveServiceDate(estimatedVehicleJourney);

    isJourneyPredictionInaccurate = TRUE.equals(estimatedVehicleJourney.isPredictionInaccurate());
    occupancy = estimatedVehicleJourney.getOccupancy();
    cancellation = TRUE.equals(estimatedVehicleJourney.isCancellation());

    calls = CallWrapper.of(estimatedVehicleJourney);

    this.transitService = transitService;
    this.getTripPatternId = getTripPatternId;
    timeZone = transitService.getTimeZone();

    stopTimesMapper = new StopTimesMapper(entityResolver, timeZone);
  }

  Result<TripUpdate, UpdateError> build() {
    if (calls.size() <= transitService.findPattern(trip).numberOfStops()) {
      // An extra call trip update is expected to have at least one more stop than the scheduled trip
      return UpdateError.result(trip.getId(), INVALID_STOP_SEQUENCE, dataSource);
    }

    if (serviceDate == null) {
      return UpdateError.result(trip.getId(), NO_START_DATE, dataSource);
    }

    FeedScopedId calServiceId = transitService.getOrCreateServiceIdForDate(serviceDate);
    if (calServiceId == null) {
      return UpdateError.result(trip.getId(), NO_START_DATE, dataSource);
    }

    ZonedDateTime departureDate = serviceDate.atStartOfDay(timeZone);

    // Create the "scheduled version" of the trip
    var aimedStopTimes = new ArrayList<StopTime>();
    for (int stopSequence = 0; stopSequence < calls.size(); stopSequence++) {
      StopTime stopTime = stopTimesMapper.createStopTime(
        trip,
        departureDate,
        stopSequence,
        calls.get(stopSequence),
        stopSequence == 0,
        stopSequence == (calls.size() - 1)
      );

      // Drop this update if the call refers to an unknown stop (not present in the site repository).
      if (stopTime == null) {
        return UpdateError.result(trip.getId(), NO_VALID_STOPS, dataSource);
      }

      aimedStopTimes.add(stopTime);
    }

    // TODO: We always create a new TripPattern to be able to modify its scheduled timetable
    StopPattern stopPattern = new StopPattern(aimedStopTimes);

    RealTimeTripTimes tripTimes = TripTimesFactory.tripTimes(
      trip,
      aimedStopTimes,
      transitService.getDeduplicator()
    );
    // validate the scheduled trip times
    // they are in general superseded by real-time trip times
    // but in case of trip cancellation, OTP will fall back to scheduled trip times
    // therefore they must be valid
    tripTimes.validateNonIncreasingTimes();
    tripTimes.setServiceCode(transitService.getServiceCode(trip.getServiceId()));

    TripPattern pattern = TripPattern.of(getTripPatternId.apply(trip))
      .withRoute(trip.getRoute())
      .withMode(trip.getMode())
      .withNetexSubmode(trip.getNetexSubMode())
      .withStopPattern(stopPattern)
      .withScheduledTimeTableBuilder(builder -> builder.addTripTimes(tripTimes))
      .withCreatedByRealtimeUpdater(true)
      .build();

    RealTimeTripTimes updatedTripTimes = tripTimes.copyScheduledTimes();

    // Loop through calls again and apply updates
    for (int stopSequence = 0; stopSequence < calls.size(); stopSequence++) {
      TimetableHelper.applyUpdates(
        departureDate,
        updatedTripTimes,
        stopSequence,
        stopSequence == (calls.size() - 1),
        isJourneyPredictionInaccurate,
        calls.get(stopSequence),
        occupancy
      );
    }

    if (cancellation || stopPattern.isAllStopsNonRoutable()) {
      updatedTripTimes.cancelTrip();
    } else {
      updatedTripTimes.setRealTimeState(RealTimeState.MODIFIED);
    }

    /* Validate */
    try {
      updatedTripTimes.validateNonIncreasingTimes();
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e, dataSource);
    }

    return Result.success(
      new TripUpdate(stopPattern, updatedTripTimes, serviceDate, null, pattern, false, dataSource)
    );
  }
}
