package org.opentripplanner.routing.algorithm.raptoradapter.transit.mappers;

import gnu.trove.set.TIntSet;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import org.opentripplanner.model.Timetable;
import org.opentripplanner.routing.algorithm.raptoradapter.transit.TripPatternForDate;
import org.opentripplanner.transit.model.timetable.FrequencyEntry;
import org.opentripplanner.transit.model.timetable.TripTimes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Maps a Timetable and a date to a TripPatternForDate. TripSchedules are then filtered according to
 * the active service codes.
 * <p>
 * If the Timetable contains a ServiceDate that is not valid for any of its trips, a message is
 * logged.
 * <p>
 * This class is THREAD SAFE because the collections initialized as part of the class state are
 * concurrent and because the collections passed in on the constructor or their elements are not
 * modified. The objects passed into the map method are also not modified.
 */
public class TripPatternForDateMapper {

  private static final Logger LOG = LoggerFactory.getLogger(TripPatternForDateMapper.class);

  private final ConcurrentMap<Timetable, List<TripTimes>> sortedTripTimesForTimetable = new ConcurrentHashMap<>();

  private final Map<LocalDate, TIntSet> serviceCodesRunningForDate;

  /**
   * @param serviceCodesRunningForDate - READ ONLY
   */
  TripPatternForDateMapper(Map<LocalDate, TIntSet> serviceCodesRunningForDate) {
    this.serviceCodesRunningForDate = Collections.unmodifiableMap(serviceCodesRunningForDate);
  }

  /**
   * This method is THREAD SAFE.
   *
   * @param timetable   The timetable to be mapped to TripPatternForDate - READ ONLY
   * @param serviceDate The date to map the TripPatternForDate for - READ ONLY
   * @return TripPatternForDate for this timetable and serviceDate
   */
  @Nullable
  public TripPatternForDate map(Timetable timetable, LocalDate serviceDate) {
    TIntSet serviceCodesRunning = serviceCodesRunningForDate.get(serviceDate);

    // ServiceCodesRunning is potentially null if the date is not present in the original GTFS.
    // At that point we can simply return null, as there are no trips running on that date.
    if (serviceCodesRunning == null) {
      LOG.debug(
        "Tried to update TripPattern {}, but no service codes are running for date {}",
        timetable.getPattern().getId(),
        serviceDate
      );

      return null;
    }

    List<TripTimes> times = new ArrayList<>();

    // The TripTimes are not sorted by departure time in the source timetable because
    // OTP1 performs a simple/ linear search. Raptor results depend on trips being
    // sorted. We reuse the same timetables many times on different days, so cache the
    // sorted versions to avoid repeated compute-intensive sorting. Anecdotally this
    // reduces mapping time by more than half, but it is still rather slow. NL Mapping
    // takes 32 seconds sorting every timetable, 9 seconds with cached sorting, and 6
    // seconds with no timetable sorting at all.
    List<TripTimes> sortedTripTimes = sortedTripTimesForTimetable.computeIfAbsent(
      timetable,
      TransitLayerMapper::getSortedTripTimes
    );

    for (TripTimes tripTimes : sortedTripTimes) {
      if (!serviceCodesRunning.contains(tripTimes.getServiceCode())) {
        continue;
      }

      if (tripTimes.isDeleted()) {
        continue;
      }

      times.add(tripTimes);
    }

    List<FrequencyEntry> frequencies = timetable
      .getFrequencyEntries()
      .stream()
      .filter(frequency -> serviceCodesRunning.contains(frequency.tripTimes.getServiceCode()))
      .sorted(Comparator.comparing(frequencyEntry -> frequencyEntry.startTime))
      .collect(Collectors.toList());

    if (times.isEmpty() && frequencies.isEmpty()) {
      if (timetable.getServiceDate() != null && timetable.getServiceDate().equals(serviceDate)) {
        LOG.debug(
          "Tried to update TripPattern {}, but no service codes are valid for date {}",
          timetable.getPattern().getId(),
          serviceDate
        );
      }
      return null;
    }

    return new TripPatternForDate(
      timetable.getPattern().getRoutingTripPattern(),
      times,
      frequencies,
      serviceDate
    );
  }
}
