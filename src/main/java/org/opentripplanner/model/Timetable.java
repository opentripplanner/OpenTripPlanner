package org.opentripplanner.model;

import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_ARRIVAL_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_DEPARTURE_TIME;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_INPUT_STRUCTURE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.INVALID_STOP_SEQUENCE;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TOO_FEW_STOPS;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND;
import static org.opentripplanner.updater.spi.UpdateError.UpdateErrorType.TRIP_NOT_FOUND_IN_PATTERN;

import com.google.transit.realtime.GtfsRealtime.TripDescriptor;
import com.google.transit.realtime.GtfsRealtime.TripUpdate;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeEvent;
import com.google.transit.realtime.GtfsRealtime.TripUpdate.StopTimeUpdate;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.UnaryOperator;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.opentripplanner.framework.time.ServiceDateUtils;
import org.opentripplanner.transit.model.framework.DataValidationException;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.Result;
import org.opentripplanner.transit.model.network.TripPattern;
import org.opentripplanner.transit.model.timetable.Direction;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.RealTimeTripTimes;
import org.opentripplanner.transit.model.timetable.Trip;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.opentripplanner.updater.GtfsRealtimeMapper;
import org.opentripplanner.updater.spi.DataValidationExceptionMapper;
import org.opentripplanner.updater.spi.UpdateError;
import org.opentripplanner.updater.trip.BackwardsDelayPropagationType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Timetables provide most of the TripPattern functionality. Each TripPattern may possess more than
 * one Timetable when stop time updates are being applied: one for the scheduled stop times, one for
 * each snapshot of updated stop times, another for a working buffer of updated stop times, etc.
 * <p>
 *  TODO OTP2 - Move this to package: org.opentripplanner.model
 *            - after as Entur NeTEx PRs are merged.
 *            - Also consider moving its dependencies in: org.opentripplanner.routing
 *            - The NEW Timetable should not have any dependencies to
 */
public class Timetable implements Serializable {

  private static final Logger LOG = LoggerFactory.getLogger(Timetable.class);

  private final TripPattern pattern;

  private final List<TripTimes> tripTimes = new ArrayList<>();

  private final List<FrequencyEntry> frequencyEntries = new ArrayList<>();

  private final LocalDate serviceDate;

  /** Construct an empty Timetable. */
  public Timetable(TripPattern pattern) {
    this.pattern = pattern;
    this.serviceDate = null;
  }

  /**
   * Copy constructor: create an un-indexed Timetable with the same TripTimes as the specified
   * timetable.
   */
  Timetable(Timetable tt, @Nonnull LocalDate serviceDate) {
    Objects.requireNonNull(serviceDate);
    tripTimes.addAll(tt.tripTimes);
    this.serviceDate = serviceDate;
    this.pattern = tt.pattern;
  }

  /** @return the index of TripTimes for this trip ID in this particular Timetable */
  public int getTripIndex(FeedScopedId tripId) {
    int ret = 0;
    for (TripTimes tt : tripTimes) {
      // could replace linear search with indexing in stoptime updater, but not necessary
      // at this point since the updater thread is far from pegged.
      if (tt.getTrip().getId().equals(tripId)) {
        return ret;
      }
      ret += 1;
    }
    return -1;
  }

  /**
   * @return the index of TripTimes for this trip ID in this particular Timetable, ignoring
   * AgencyIds.
   */
  public int getTripIndex(String tripId) {
    int ret = 0;
    for (TripTimes tt : tripTimes) {
      if (tt.getTrip().getId().getId().equals(tripId)) {
        return ret;
      }
      ret += 1;
    }
    return -1;
  }

  public TripTimes getTripTimes(int tripIndex) {
    return tripTimes.get(tripIndex);
  }

  @Nullable
  public TripTimes getTripTimes(Trip trip) {
    for (TripTimes tt : tripTimes) {
      if (tt.getTrip() == trip) {
        return tt;
      }
    }
    return null;
  }

  public TripTimes getTripTimes(FeedScopedId tripId) {
    for (TripTimes tt : tripTimes) {
      if (tt.getTrip().getId() == tripId) {
        return tt;
      }
    }
    return null;
  }

  /**
   * Set new trip times for trip given a trip index
   *
   * @param tripIndex trip index of trip
   * @param tt        new trip times for trip
   * @return old trip times of trip
   */
  public TripTimes setTripTimes(int tripIndex, TripTimes tt) {
    return tripTimes.set(tripIndex, tt);
  }

  /**
   * Apply the TripUpdate to the appropriate TripTimes from this Timetable. The existing TripTimes
   * must not be modified directly because they may be shared with the underlying
   * scheduledTimetable, or other updated Timetables. The {@link TimetableSnapshot} performs the
   * protective copying of this Timetable. It is not done in this update method to avoid repeatedly
   * cloning the same Timetable when several updates are applied to it at once. We assume here that
   * all trips in a timetable are from the same feed, which should always be the case.
   *
   * @param tripUpdate                    GTFS-RT trip update
   * @param timeZone                      time zone of trip update
   * @param updateServiceDate             service date of trip update
   * @param backwardsDelayPropagationType Defines when delays are propagated to previous stops and
   *                                      if these stops are given the NO_DATA flag
   * @return {@link Result<TripTimesPatch,   UpdateError  >} contains either a new copy of updated
   * TripTimes after TripUpdate has been applied on TripTimes of trip with the id specified in the
   * trip descriptor of the TripUpdate and a list of stop indices that have been skipped with the
   * realtime update; or an error if something went wrong
   * <p>
   * TODO OTP2 - This method depend on GTFS RealTime classes. Refactor this so GTFS RT can do
   *           - its job without sending in GTFS specific classes. A generic update would support
   *           - other RealTime updats, not just from GTFS.
   */
  public Result<TripTimesPatch, UpdateError> createUpdatedTripTimesFromGTFSRT(
    TripUpdate tripUpdate,
    ZoneId timeZone,
    LocalDate updateServiceDate,
    BackwardsDelayPropagationType backwardsDelayPropagationType
  ) {
    Result<TripTimesPatch, UpdateError> invalidInput = Result.failure(
      UpdateError.noTripId(INVALID_INPUT_STRUCTURE)
    );
    if (tripUpdate == null) {
      LOG.debug("A null TripUpdate pointer was passed to the Timetable class update method.");
      return invalidInput;
    }

    // Though all timetables have the same trip ordering, some may have extra trips due to
    // the dynamic addition of unscheduled trips.
    // However, we want to apply trip updates on top of *scheduled* times
    if (!tripUpdate.hasTrip()) {
      LOG.debug("TripUpdate object has no TripDescriptor field.");
      return invalidInput;
    }

    TripDescriptor tripDescriptor = tripUpdate.getTrip();
    if (!tripDescriptor.hasTripId()) {
      LOG.debug("TripDescriptor object has no TripId field");
      Result.failure(UpdateError.noTripId(TRIP_NOT_FOUND));
    }

    String tripId = tripDescriptor.getTripId();

    var feedScopedTripId = new FeedScopedId(this.getPattern().getFeedId(), tripId);

    int tripIndex = getTripIndex(tripId);
    if (tripIndex == -1) {
      LOG.debug("tripId {} not found in pattern.", tripId);
      return Result.failure(new UpdateError(feedScopedTripId, TRIP_NOT_FOUND_IN_PATTERN));
    } else {
      LOG.trace("tripId {} found at index {} in timetable.", tripId, tripIndex);
    }

    RealTimeTripTimes newTimes = getTripTimes(tripIndex).copyScheduledTimes();
    List<Integer> skippedStopIndices = new ArrayList<>();

    // The GTFS-RT reference specifies that StopTimeUpdates are sorted by stop_sequence.
    Iterator<StopTimeUpdate> updates = tripUpdate.getStopTimeUpdateList().iterator();
    if (!updates.hasNext()) {
      LOG.warn("Won't apply zero-length trip update to trip {}.", tripId);
      return Result.failure(new UpdateError(feedScopedTripId, TOO_FEW_STOPS));
    }
    StopTimeUpdate update = updates.next();

    int numStops = newTimes.getNumStops();
    Integer delay = null;
    Integer firstUpdatedIndex = null;

    final long today = ServiceDateUtils
      .asStartOfService(updateServiceDate, timeZone)
      .toEpochSecond();

    for (int i = 0; i < numStops; i++) {
      boolean match = false;
      if (update != null) {
        if (update.hasStopSequence()) {
          match = update.getStopSequence() == newTimes.gtfsSequenceOfStopIndex(i);
        } else if (update.hasStopId()) {
          match = pattern.getStop(i).getId().getId().equals(update.getStopId());
        }
      }

      if (match) {
        StopTimeUpdate.ScheduleRelationship scheduleRelationship = update.hasScheduleRelationship()
          ? update.getScheduleRelationship()
          : StopTimeUpdate.ScheduleRelationship.SCHEDULED;
        // Handle each schedule relationship case
        if (scheduleRelationship == StopTimeUpdate.ScheduleRelationship.SKIPPED) {
          // Set status to cancelled and delays to previously recorded delays or to 0 otherwise.
          // Note: This will discard the times from TripUpdates even if they are present.
          skippedStopIndices.add(i);
          newTimes.setCancelled(i);
          int delayOrZero = delay != null ? delay : 0;
          newTimes.updateArrivalDelay(i, delayOrZero);
          newTimes.updateDepartureDelay(i, delayOrZero);
        } else if (scheduleRelationship == StopTimeUpdate.ScheduleRelationship.NO_DATA) {
          // Set status to NO_DATA and delays to 0.
          // Note: GTFS-RT requires NO_DATA stops to have no arrival departure times.
          newTimes.updateArrivalDelay(i, 0);
          newTimes.updateDepartureDelay(i, 0);
          delay = 0;
          newTimes.setNoData(i);
        } else {
          // Else the status is SCHEDULED, update times as needed.
          if (update.hasArrival()) {
            if (firstUpdatedIndex == null) {
              firstUpdatedIndex = i;
            }
            StopTimeEvent arrival = update.getArrival();
            if (arrival.hasDelay()) {
              delay = arrival.getDelay();
              if (arrival.hasTime()) {
                newTimes.updateArrivalTime(i, (int) (arrival.getTime() - today));
              } else {
                newTimes.updateArrivalDelay(i, delay);
              }
            } else if (arrival.hasTime()) {
              newTimes.updateArrivalTime(i, (int) (arrival.getTime() - today));
              delay = newTimes.getArrivalDelay(i);
            } else {
              LOG.debug(
                "Arrival time at index {} of trip {} has neither a delay nor a time.",
                i,
                feedScopedTripId
              );
              return Result.failure(new UpdateError(feedScopedTripId, INVALID_ARRIVAL_TIME, i));
            }
          } else if (delay != null) {
            newTimes.updateArrivalDelay(i, delay);
          }

          if (update.hasDeparture()) {
            if (firstUpdatedIndex == null) {
              firstUpdatedIndex = i;
            }
            StopTimeEvent departure = update.getDeparture();
            if (departure.hasDelay()) {
              delay = departure.getDelay();
              if (departure.hasTime()) {
                newTimes.updateDepartureTime(i, (int) (departure.getTime() - today));
              } else {
                newTimes.updateDepartureDelay(i, delay);
              }
            } else if (departure.hasTime()) {
              newTimes.updateDepartureTime(i, (int) (departure.getTime() - today));
              delay = newTimes.getDepartureDelay(i);
            } else {
              LOG.debug(
                "Departure time at index {} of trip {} has neither a delay nor a time.",
                i,
                feedScopedTripId
              );
              return Result.failure(new UpdateError(feedScopedTripId, INVALID_DEPARTURE_TIME, i));
            }
          } else if (delay != null) {
            newTimes.updateDepartureDelay(i, delay);
          }
        }

        if (updates.hasNext()) {
          update = updates.next();
        } else {
          update = null;
        }
      } else if (delay != null) {
        // If not match and has previously set delays, propagate delays.
        newTimes.updateArrivalDelay(i, delay);
        newTimes.updateDepartureDelay(i, delay);
      }
    }
    if (update != null) {
      LOG.debug(
        "Part of a TripUpdate object could not be applied successfully to trip {}.",
        tripId
      );
      return Result.failure(new UpdateError(feedScopedTripId, INVALID_STOP_SEQUENCE));
    }

    // Backwards propagation for past stops that are no longer present in GTFS-RT, that is, up until
    // the first SCHEDULED stop sequence included in the GTFS-RT feed.
    if (firstUpdatedIndex != null && firstUpdatedIndex > 0) {
      if (
        (
          backwardsDelayPropagationType == BackwardsDelayPropagationType.REQUIRED_NO_DATA &&
          newTimes.adjustTimesBeforeWhenRequired(firstUpdatedIndex, true)
        ) ||
        (
          backwardsDelayPropagationType == BackwardsDelayPropagationType.REQUIRED &&
          newTimes.adjustTimesBeforeWhenRequired(firstUpdatedIndex, false)
        ) ||
        (
          backwardsDelayPropagationType == BackwardsDelayPropagationType.ALWAYS &&
          newTimes.adjustTimesBeforeAlways(firstUpdatedIndex)
        )
      ) {
        LOG.debug(
          "Propagated delay from stop index {} backwards on trip {}.",
          firstUpdatedIndex,
          tripId
        );
      }
    }

    // Interpolate missing times from SKIPPED stops since they don't necessarily have times
    // associated. Note: Currently for GTFS-RT updates ONLY not for SIRI updates.
    if (newTimes.interpolateMissingTimes()) {
      LOG.debug("Interpolated delays for cancelled stops on trip {}.", tripId);
    }

    // Validate for non-increasing times. Log error if present.
    try {
      newTimes.validateNonIncreasingTimes();
    } catch (DataValidationException e) {
      return DataValidationExceptionMapper.toResult(e);
    }

    if (tripUpdate.hasVehicle()) {
      var vehicleDescriptor = tripUpdate.getVehicle();
      if (vehicleDescriptor.hasWheelchairAccessible()) {
        GtfsRealtimeMapper
          .mapWheelchairAccessible(vehicleDescriptor.getWheelchairAccessible())
          .ifPresent(newTimes::updateWheelchairAccessibility);
      }
    }

    LOG.trace(
      "A valid TripUpdate object was applied to trip {} using the Timetable class update method.",
      tripId
    );
    return Result.success(new TripTimesPatch(newTimes, skippedStopIndices));
  }

  /**
   * Add a trip to this Timetable. The Timetable must be analyzed, compacted, and indexed any time
   * trips are added, but this is not done automatically because it is time consuming and should
   * only be done once after an entire batch of trips are added. Note that the trip is not added to
   * the enclosing pattern here, but in the pattern's wrapper function. Here we don't know if it's a
   * scheduled trip or a realtime-added trip.
   */
  public void addTripTimes(TripTimes tt) {
    tripTimes.add(tt);
  }

  /**
   * Apply the same update to all trip-times inculuding scheduled and frequency based
   * trip times.
   * <p>
   * THIS IS NOT THREAD-SAFE - ONLY USE THIS METHOD DURING GRAPH-BUILD!
   */
  public void updateAllTripTimes(UnaryOperator<TripTimes> update) {
    tripTimes.replaceAll(update);
    frequencyEntries.replaceAll(it ->
      new FrequencyEntry(
        it.startTime,
        it.endTime,
        it.headway,
        it.exactTimes,
        update.apply(it.tripTimes)
      )
    );
  }

  /**
   * Add a frequency entry to this Timetable. See addTripTimes method. Maybe Frequency Entries
   * should just be TripTimes for simplicity.
   */
  public void addFrequencyEntry(FrequencyEntry freq) {
    frequencyEntries.add(freq);
  }

  public boolean isValidFor(LocalDate serviceDate) {
    return this.serviceDate == null || this.serviceDate.equals(serviceDate);
  }

  /** Find and cache service codes. Duplicates information in trip.getServiceId for optimization. */
  // TODO maybe put this is a more appropriate place
  public void setServiceCodes(Map<FeedScopedId, Integer> serviceCodes) {
    for (TripTimes tt : this.tripTimes) {
      ((RealTimeTripTimes) tt).setServiceCode(serviceCodes.get(tt.getTrip().getServiceId()));
    }
    // Repeated code... bad sign...
    for (FrequencyEntry freq : this.frequencyEntries) {
      TripTimes tt = freq.tripTimes;
      ((RealTimeTripTimes) tt).setServiceCode(serviceCodes.get(tt.getTrip().getServiceId()));
    }
  }

  /**
   * A circular reference between TripPatterns and their scheduled (non-updated) timetables.
   */
  public TripPattern getPattern() {
    return pattern;
  }

  /**
   * Contains one TripTimes object for each scheduled trip (even cancelled ones) and possibly
   * additional TripTimes objects for unscheduled trips. Frequency entries are stored separately.
   */
  public List<TripTimes> getTripTimes() {
    return tripTimes;
  }

  /**
   * Contains one FrequencyEntry object for each block of frequency-based trips.
   */
  public List<FrequencyEntry> getFrequencyEntries() {
    return frequencyEntries;
  }

  /**
   * The ServiceDate for which this (updated) timetable is valid. If null, then it is valid for all
   * dates.
   */
  @Nullable
  public LocalDate getServiceDate() {
    return serviceDate;
  }

  /**
   * The direction for all the trips in this pattern.
   */
  public Direction getDirection() {
    return Optional
      .ofNullable(getRepresentativeTripTimes())
      .map(TripTimes::getTrip)
      .map(Trip::getDirection)
      .orElse(Direction.UNKNOWN);
  }

  public TripTimes getRepresentativeTripTimes() {
    if (!getTripTimes().isEmpty()) {
      return getTripTimes(0);
    } else if (!getFrequencyEntries().isEmpty()) {
      return getFrequencyEntries().get(0).tripTimes;
    } else {
      // Pattern is created only for real-time updates
      return null;
    }
  }
}
