package org.opentripplanner.updater.trip.gtfs;

import static org.opentripplanner.updater.spi.UpdateErrorType.NOT_IMPLEMENTED_DUPLICATED;
import static org.opentripplanner.updater.spi.UpdateErrorType.NOT_IMPLEMENTED_UNSCHEDULED;
import static org.opentripplanner.updater.trip.UpdateIncrementality.FULL_DATASET;

import com.google.transit.realtime.GtfsRealtime;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import org.opentripplanner.core.framework.deduplicator.DeduplicatorService;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.service.DefaultTransitService;
import org.opentripplanner.transit.service.TimetableRepository;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.ResultLogger;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.spi.UpdateException;
import org.opentripplanner.updater.spi.UpdateResult;
import org.opentripplanner.updater.spi.UpdateSuccess;
import org.opentripplanner.updater.trip.TimetableSnapshotManager;
import org.opentripplanner.updater.trip.UpdateIncrementality;
import org.opentripplanner.updater.trip.gtfs.interpolation.BackwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.interpolation.ForwardsDelayPropagationType;
import org.opentripplanner.updater.trip.gtfs.model.TripUpdate;
import org.opentripplanner.updater.trip.patterncache.TripPatternCache;
import org.opentripplanner.updater.trip.patterncache.TripPatternIdGenerator;

/**
 * Adapts from GTFS-RT TripUpdates to OTP's internal real-time data model.
 */
public class GtfsRealTimeTripUpdateAdapter {

  private final TimetableSnapshotManager snapshotManager;
  private final Supplier<LocalDate> localDateNow;
  private final ScheduledTripHandler scheduledTripHandler;
  private final NewTripHandler addedTripHandler;
  private final CanceledTripHandler canceledTripHandler;

  /**
   * Constructor to allow tests to provide their own clock, not using system time.
   */
  public GtfsRealTimeTripUpdateAdapter(
    TimetableRepository timetableRepository,
    DeduplicatorService deduplicator,
    TimetableSnapshotManager snapshotManager,
    Supplier<LocalDate> localDateNow
  ) {
    this.snapshotManager = snapshotManager;
    this.localDateNow = localDateNow;

    var transitEditorService = new DefaultTransitService(
      timetableRepository,
      snapshotManager.getTimetableSnapshotBuffer()
    );
    var tripTimesUpdater = new TripTimesUpdater(timetableRepository.getTimeZone(), deduplicator);
    var tripPatternCache = new TripPatternCache(
      new TripPatternIdGenerator(),
      transitEditorService::findPattern
    );

    this.scheduledTripHandler = new ScheduledTripHandler(
      transitEditorService,
      snapshotManager,
      tripTimesUpdater,
      tripPatternCache
    );
    this.addedTripHandler = new NewTripHandler(
      transitEditorService,
      snapshotManager,
      tripTimesUpdater,
      tripPatternCache
    );
    this.canceledTripHandler = new CanceledTripHandler(transitEditorService, snapshotManager);
  }

  /**
   * Method to apply a trip update list to the most recent version of the timetable snapshot. A
   * GTFS-RT feed is always applied against a single static feed (indicated by feedId).
   * <p>
   * However, multi-feed support is not completed and we currently assume there is only one static
   * feed when matching IDs.
   *
   * @param backwardsDelayPropagationType Defines when delays are propagated to previous stops and
   *                                      if these stops are given the NO_DATA flag.
   * @param updateIncrementality          Determines the incrementality of the updates. FULL updates clear the buffer
   *                                      of all previous updates for the given feed id.
   * @param updates                       GTFS-RT TripUpdate's that should be applied atomically
   */
  public UpdateResult applyTripUpdates(
    @Nullable GtfsRealtimeFuzzyTripMatcher fuzzyTripMatcher,
    ForwardsDelayPropagationType forwardsDelayPropagationType,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    UpdateIncrementality updateIncrementality,
    List<GtfsRealtime.TripUpdate> updates,
    String feedId
  ) {
    List<UpdateSuccess> successes = new ArrayList<>();
    List<UpdateError> errors = new ArrayList<>();

    if (updateIncrementality == FULL_DATASET) {
      // Remove all updates from the buffer
      snapshotManager.clearBuffer(feedId);
    }

    for (var rawTripUpdate : updates) {
      UpdateSuccess result;
      try {
        if (fuzzyTripMatcher != null) {
          var trip = fuzzyTripMatcher.match(feedId, rawTripUpdate.getTrip());
          rawTripUpdate = rawTripUpdate.toBuilder().setTrip(trip).build();
        }

        var tripUpdate = new TripUpdate(feedId, rawTripUpdate, localDateNow);
        tripUpdate.validate();

        result = applyUpdate(
          tripUpdate,
          updateIncrementality,
          backwardsDelayPropagationType,
          forwardsDelayPropagationType
        );
        successes.add(result);
      } catch (DataValidationException e) {
        errors.add(DataValidationExceptionMapper.map(e).toError());
      } catch (UpdateException e) {
        errors.add(e.toError());
      }
    }

    var updateResult = UpdateResult.of(successes, errors);

    if (updateIncrementality == FULL_DATASET) {
      ResultLogger.logUpdateResult(feedId, "gtfs-rt-trip-updates", updateResult);
    }
    return updateResult;
  }

  private UpdateSuccess applyUpdate(
    TripUpdate tripUpdate,
    UpdateIncrementality updateIncrementality,
    BackwardsDelayPropagationType backwardsDelayPropagationType,
    ForwardsDelayPropagationType forwardsDelayPropagationType
  ) throws UpdateException {
    // The GTFS-RT TripDescriptor.schedule_relationship field is a protobuf optional enum,
    // so a single TripUpdate message carries exactly one value — it is structurally impossible
    // for a message to express two states (e.g. ADDED and CANCELED) at the same time.
    // Cancelling a previously-added trip therefore always arrives as a second, separate feed
    // entity carrying only CANCELED or DELETED. This is why the RealTimeTripTimesBuilder never
    // needs to hold both added=true and canceled=true simultaneously for a GTFS-RT source.
    return switch (tripUpdate.scheduleRelationship()) {
      case SCHEDULED -> scheduledTripHandler.handle(
        tripUpdate,
        forwardsDelayPropagationType,
        backwardsDelayPropagationType
      );
      case NEW, ADDED -> addedTripHandler.handleNew(tripUpdate);
      case CANCELED -> canceledTripHandler.cancel(tripUpdate, updateIncrementality);
      case DELETED -> canceledTripHandler.delete(tripUpdate, updateIncrementality);
      case REPLACEMENT -> addedTripHandler.handleReplacement(tripUpdate);
      case UNSCHEDULED -> throw UpdateException.of(
        tripUpdate.tripId(),
        NOT_IMPLEMENTED_UNSCHEDULED
      );
      case DUPLICATED -> throw UpdateException.of(tripUpdate.tripId(), NOT_IMPLEMENTED_DUPLICATED);
    };
  }
}
