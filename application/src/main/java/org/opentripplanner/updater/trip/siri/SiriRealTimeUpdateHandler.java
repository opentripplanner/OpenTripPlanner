package org.opentripplanner.updater.trip.siri;

import static org.opentripplanner.updater.spi.UpdateErrorType.NO_START_DATE;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;
import static org.opentripplanner.updater.spi.UpdateErrorType.UNKNOWN;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimesBuilder;
import org.opentripplanner.transit.model.timetable.RealTimeTripUpdate;
import org.opentripplanner.transit.model.timetable.Timetable;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.transit.repository.MutableTimetableSnapshot;
import org.opentripplanner.transit.service.TransitEditorService;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TripUpdateApplier;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;
import org.opentripplanner.updater.trip.patterncache.TripPatternIdGenerator;
import org.opentripplanner.updater.trip.siri.support.TripReferenceHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.org.siri.siri21.EstimatedTimetableDeliveryStructure;
import uk.org.siri.siri21.EstimatedVehicleJourney;

/**
 * Update-scoped object produced by {@link SiriRealTimeTripUpdateAdapter#forUpdate}. Holds a
 * per-task {@link TransitEditorService} backed by the update's mutable timetable snapshot, so all
 * pattern and trip lookups within the task see in-progress real-time additions.
 */
public class SiriRealTimeUpdateHandler {

  private static final Logger LOG = LoggerFactory.getLogger(SiriRealTimeUpdateHandler.class);

  private final TransitEditorService transitEditorService;
  private final MutableTimetableSnapshot buffer;

  @Nullable
  private final SiriFuzzyTripMatcher fuzzyTripMatcher;

  private final TripPatternCache tripPatternCache;
  private final DeduplicatorService deduplicator;
  private final TripPatternIdGenerator tripPatternIdGenerator;

  SiriRealTimeUpdateHandler(
    TransitEditorService transitEditorService,
    MutableTimetableSnapshot buffer,
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    TripPatternCache tripPatternCache,
    DeduplicatorService deduplicator,
    TripPatternIdGenerator tripPatternIdGenerator
  ) {
    this.transitEditorService = transitEditorService;
    this.buffer = buffer;
    this.fuzzyTripMatcher = fuzzyTripMatcher;
    this.tripPatternCache = tripPatternCache;
    this.deduplicator = deduplicator;
    this.tripPatternIdGenerator = tripPatternIdGenerator;
  }

  /**
   * Method to apply estimated timetables to the most recent version of the timetable snapshot.
   *
   * @param incrementality  the incrementality of the update, for example if updates represent all
   *                        updates that are active right now, i.e. all previous updates should be
   *                        disregarded
   * @param updates    SIRI EstimatedTimetable deliveries that should be applied atomically.
   */
  public UpdateResult applyEstimatedTimetable(
    EntityResolver entityResolver,
    String feedId,
    UpdateIncrementality incrementality,
    List<EstimatedTimetableDeliveryStructure> updates
  ) {
    if (updates == null) {
      LOG.warn("updates is null");
      return UpdateResult.empty();
    }

    List<UpdateSuccess> successes = new ArrayList<>();
    List<UpdateError> errors = new ArrayList<>();

    if (incrementality == FULL_DATASET) {
      // Remove all updates from the buffer
      buffer.clear(feedId);
    }

    for (var etDelivery : updates) {
      for (var estimatedJourneyVersion : etDelivery.getEstimatedJourneyVersionFrames()) {
        var journeys = estimatedJourneyVersion.getEstimatedVehicleJourneies();
        LOG.debug("Handling {} EstimatedVehicleJourneys.", journeys.size());
        for (EstimatedVehicleJourney journey : journeys) {
          try {
            successes.add(apply(journey, transitEditorService, entityResolver));
          } catch (UpdateException e) {
            errors.add(
              e
                .withTripReference(TripReferenceHelper.tripReference(journey))
                .toError(journey.getDataSource())
            );
          }
        }
      }
    }

    LOG.debug("message contains {} trip updates", updates.size());

    return UpdateResult.of(successes, errors);
  }

  private UpdateSuccess apply(
    EstimatedVehicleJourney journey,
    TransitEditorService transitService,
    EntityResolver entityResolver
  ) throws UpdateException {
    var journeyWrapper = EstimatedVehicleJourneyWrapper.of(journey);
    SiriUpdateType siriUpdateType = null;
    try {
      siriUpdateType = updateType(journeyWrapper, entityResolver);
      TripUpdate result = switch (siriUpdateType) {
        case REPLACEMENT_DEPARTURE -> new AddedTripBuilder(
          journeyWrapper,
          transitService,
          deduplicator,
          entityResolver,
          tripPatternIdGenerator::generateUniqueTripPatternId
        ).build();
        case EXTRA_CALL -> handleExtraCall(fuzzyTripMatcher, entityResolver, journeyWrapper);
        case TRIP_UPDATE -> handleModifiedTrip(fuzzyTripMatcher, entityResolver, journeyWrapper);
      };

      /* commit */
      return addTripToGraphAndBuffer(result);
    } catch (UpdateException e) {
      throw e;
    } catch (DataValidationException e) {
      throw DataValidationExceptionMapper.map(e);
    } catch (Exception e) {
      LOG.warn("{} EstimatedJourney {} failed.", siriUpdateType, journeyWrapper, e);
      throw UpdateException.noTripId(UNKNOWN);
    }
  }

  /**
   * Determines the type of SIRI-ET update carried by {@code vehicleJourney}.
   *
   * <h2>Why ExtraJourney and Cancellation are never both true in the same message</h2>
   *
   * In the SIRI 2.0/2.1 XSD (and in the Nordic SIRI profile), the {@code ExtraJourney} and
   * {@code Cancellation} elements of {@code EstimatedVehicleJourney} are enclosed in an
   * {@code <xsd:choice>} group:
   *
   * <pre>{@code
   * <xsd:choice>
   *   <xsd:element name="ExtraJourney"  type="xsd:boolean" minOccurs="0"/>
   *   <xsd:element name="Cancellation" type="xsd:boolean" minOccurs="0"/>
   * </xsd:choice>
   * }</pre>
   *
   * This means a single {@code EstimatedVehicleJourney} is schema-invalid if it contains both
   * elements. Cancelling a previously-added extra journey therefore always arrives as a second,
   * separate {@code ServiceDelivery} that carries only {@code Cancellation=true} (and no
   * {@code ExtraJourney} element). That second message is routed here as {@code TRIP_UPDATE}
   * (because {@code isExtraJourney()} is {@code null}/false), and {@code ModifiedTripBuilder}
   * handles the cancellation.
   *
   * <p>This is why the {@link RealTimeTripTimesBuilder} never needs to hold both
   * {@code added=true} and {@code canceled=true} at the same time for a SIRI source.
   */
  private SiriUpdateType updateType(
    EstimatedVehicleJourneyWrapper journey,
    EntityResolver entityResolver
  ) {
    // Extra call if at least one of the call is an extra call
    if (journey.hasExtraCall()) {
      return SiriUpdateType.EXTRA_CALL;
    }

    // Replacement departure if the trip is marked as extra journey, and it has not been added before
    if (journey.isExtraJourney() && entityResolver.resolveTrip(journey) == null) {
      return SiriUpdateType.REPLACEMENT_DEPARTURE;
    }

    // otherwise this is a trip update
    return SiriUpdateType.TRIP_UPDATE;
  }

  /**
   * Get the latest timetable for TripPattern for a given service date.
   * <p>
   * Snapshot timetable is used as source if initialised, trip patterns scheduled timetable if not.
   */
  private Timetable getCurrentTimetable(TripPattern tripPattern, LocalDate serviceDate) {
    return buffer.resolve(tripPattern, serviceDate);
  }

  private TripUpdate handleModifiedTrip(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourneyWrapper journey
  ) throws UpdateException {
    Trip trip = entityResolver.resolveTrip(journey);

    LocalDate serviceDate = entityResolver.resolveServiceDate(journey);

    if (serviceDate == null) {
      throw UpdateException.of(trip != null ? trip.getId() : null, NO_START_DATE);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitEditorService.findPattern(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      var tripAndPattern = fuzzyTripMatcher.match(
        journey,
        entityResolver,
        this::getCurrentTimetable,
        buffer::getNewTripPatternForModifiedTrip
      );
      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      throw UpdateException.of(null, TRIP_NOT_FOUND);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      throw UpdateException.of(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN);
    }
    var tripUpdate = new ModifiedTripBuilder(
      existingTripTimes,
      pattern,
      journey,
      serviceDate,
      transitEditorService.getTimeZone(),
      entityResolver
    ).build();

    TripPattern deleteFrom = !tripUpdate.stopPattern().equals(pattern.getStopPattern())
      ? pattern
      : null;

    return tripUpdate.withHideTripInScheduledPattern(deleteFrom);
  }

  private TripUpdate handleExtraCall(
    @Nullable SiriFuzzyTripMatcher fuzzyTripMatcher,
    EntityResolver entityResolver,
    EstimatedVehicleJourneyWrapper journey
  ) throws UpdateException {
    Trip trip = entityResolver.resolveTrip(journey);

    LocalDate serviceDate = entityResolver.resolveServiceDate(journey);

    if (serviceDate == null) {
      throw UpdateException.of(trip != null ? trip.getId() : null, NO_START_DATE);
    }

    TripPattern pattern;

    if (trip != null) {
      // Found exact match
      pattern = transitEditorService.findPattern(trip);
    } else if (fuzzyTripMatcher != null) {
      // No exact match found - search for trips based on arrival-times/stop-patterns
      var tripAndPattern = fuzzyTripMatcher.match(
        journey,
        entityResolver,
        this::getCurrentTimetable,
        buffer::getNewTripPatternForModifiedTrip
      );

      trip = tripAndPattern.trip();
      pattern = tripAndPattern.tripPattern();
    } else {
      throw UpdateException.of(null, TRIP_NOT_FOUND);
    }

    Timetable currentTimetable = getCurrentTimetable(pattern, serviceDate);
    TripTimes existingTripTimes = currentTimetable.getTripTimes(trip);
    if (existingTripTimes == null) {
      LOG.debug("tripId {} not found in pattern.", trip.getId());
      throw UpdateException.of(trip.getId(), TRIP_NOT_FOUND_IN_PATTERN);
    }
    var tripUpdate = new ExtraCallTripBuilder(
      journey,
      transitEditorService,
      deduplicator,
      entityResolver,
      tripPatternIdGenerator::generateUniqueTripPatternId,
      trip
    ).build();

    TripPattern deleteFrom = !tripUpdate.stopPattern().equals(pattern.getStopPattern())
      ? pattern
      : null;

    return tripUpdate.withHideTripInScheduledPattern(deleteFrom);
  }

  /**
   * Add a (new) trip to the timetableRepository and the buffer
   */
  private UpdateSuccess addTripToGraphAndBuffer(TripUpdate tripUpdate) {
    Trip trip = tripUpdate.tripTimes().getTrip();
    LocalDate serviceDate = tripUpdate.serviceDate();

    final TripPattern pattern;
    if (tripUpdate.tripPatternCreation()) {
      pattern = tripUpdate.addedTripPattern();
    } else {
      // Get cached trip pattern or create one if it doesn't exist yet
      pattern = tripPatternCache.getOrCreateTripPattern(
        tripUpdate.stopPattern(),
        trip,
        transitEditorService.findPattern(trip)
      );
    }

    // Revert for TRIP_UPDATE and EXTRA_CALL, but NOT for REPLACEMENT_DEPARTURE (new trips)
    boolean revertPreviousRealTimeUpdates = !tripUpdate.tripCreation();

    // Add new trip times to buffer, making protective copies as needed. Bubble success/error up.
    RealTimeTripUpdate realTimeTripUpdate = RealTimeTripUpdate.of(
      pattern,
      tripUpdate.tripTimes(),
      serviceDate
    )
      .withAddedTripOnServiceDate(tripUpdate.addedTripOnServiceDate())
      .withTripCreation(tripUpdate.tripCreation())
      .withRouteCreation(tripUpdate.routeCreation())
      .withProducer(tripUpdate.dataSource())
      .withRevertPreviousRealTimeUpdates(revertPreviousRealTimeUpdates)
      .withHideTripInScheduledPattern(tripUpdate.hideTripInScheduledPattern())
      .build();
    var result = TripUpdateApplier.apply(buffer, realTimeTripUpdate);
    LOG.debug("Applied real-time data for trip {} on {}", trip, serviceDate);
    return result;
  }

  /**
   * Types of SIRI update messages.
   */
  private enum SiriUpdateType {
    /**
     * Update of an existing trip.
     * This can be either a trip defined in planned data or a replacement departure
     * that was previously added by a real-time message.
     * The update can consist in updated passing times and/or cancellation of some stops.
     * A stop can be substituted by another if they belong to the same station.
     * The whole trip can also be marked as cancelled.
     */
    TRIP_UPDATE,

    /**
     * Addition of a new trip, not currently present in the system.
     * The new trip has a new unique id.
     * The trip can replace one or more existing trips, another SIRI message should handle the
     * cancellation of the replaced trips.
     */
    REPLACEMENT_DEPARTURE,

    /**
     * Addition of one or more stops in an existing trip.
     */
    EXTRA_CALL,
  }
}
