package org.opentripplanner.transit.model.timetable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import org.opentripplanner.core.model.i18n.I18NString;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.transit.model.framework.DeduplicatorService;
import org.opentripplanner.transit.model.timetable.booking.BookingInfo;

class StopTimeToScheduledTripTimesMapper {

  private final Trip trip;
  private final ScheduledTripTimesBuilder builder;

  private static final String[] EMPTY_STRING_ARRAY = new String[0];

  private StopTimeToScheduledTripTimesMapper(Trip trip, DeduplicatorService deduplicator) {
    this.trip = trip;
    this.builder = ScheduledTripTimes.of(deduplicator).withTrip(trip);
  }

  /**
   * The provided stopTimes are assumed to be pre-filtered, valid, and monotonically increasing.
   * The non-interpolated stoptimes should already be marked at timepoints by a previous filtering
   * step.
   */
  public static ScheduledTripTimes map(
    Trip trip,
    Collection<StopTime> stopTimes,
    DeduplicatorService deduplicator
  ) {
    return new StopTimeToScheduledTripTimesMapper(trip, deduplicator).doMap(stopTimes);
  }

  private ScheduledTripTimes doMap(Collection<StopTime> stopTimes) {
    final int nStops = stopTimes.size();
    final int[] departures = new int[nStops];
    final int[] arrivals = new int[nStops];
    final int[] sequences = new int[nStops];
    final BitSet timepoints = new BitSet(nStops);

    final List<BookingInfo> dropOffBookingInfos = new ArrayList<>();
    final List<BookingInfo> pickupBookingInfos = new ArrayList<>();
    int s = 0;
    for (final StopTime st : stopTimes) {
      departures[s] = st.getDepartureTime();
      arrivals[s] = st.getArrivalTime();
      sequences[s] = st.getStopSequence();
      timepoints.set(s, st.getTimepoint() == 1);

      dropOffBookingInfos.add(st.getDropOffBookingInfo());
      pickupBookingInfos.add(st.getPickupBookingInfo());
      s++;
    }
    builder
      .withDepartureTimes(departures)
      .withArrivalTimes(arrivals)
      .withGtfsSequenceOfStopIndex(sequences)
      .withHeadsigns(makeHeadsignsArray(stopTimes))
      .withHeadsignVias(makeHeadsignViasArray(stopTimes))
      .withDropOffBookingInfos(dropOffBookingInfos)
      .withPickupBookingInfos(pickupBookingInfos)
      .withTimepoints(timepoints);

    return builder.build();
  }

  /**
   * @return either an array of headsigns (one for each stop on this trip) or null if the headsign
   * is the same at all stops (including null) and can be found in the Trip object.
   */
  private I18NString[] makeHeadsignsArray(final Collection<StopTime> stopTimes) {
    final I18NString tripHeadsign = trip.getHeadsign();
    boolean useStopHeadsigns = false;
    if (tripHeadsign == null) {
      useStopHeadsigns = true;
    } else {
      for (final StopTime st : stopTimes) {
        if (!(tripHeadsign.equals(st.getStopHeadsign()))) {
          useStopHeadsigns = true;
          break;
        }
      }
    }
    if (!useStopHeadsigns) {
      // defer to trip_headsign
      return null;
    }
    boolean allNull = true;
    int i = 0;
    final I18NString[] hs = new I18NString[stopTimes.size()];
    for (final StopTime st : stopTimes) {
      final I18NString headsign = st.getStopHeadsign();
      hs[i++] = headsign;
      if (headsign != null) {
        allNull = false;
      }
    }
    if (allNull) {
      return null;
    } else {
      return hs;
    }
  }

  /**
   * Create 2D array of String containing zero or more Via messages displayed at each stop in the
   * stop sequence.
   * @return May be null if no stop in the entire sequence of stops has any via strings. Any
   * subarray may also be null or empty if no Via strings are displayed at that particular stop.
   * @see org.opentripplanner.transit.model.timetable.TripTimes#getHeadsignVias(int)
   */
  @Nullable
  private String[][] makeHeadsignViasArray(final Collection<StopTime> stopTimes) {
    if (
      stopTimes
        .stream()
        .allMatch(st -> st.getHeadsignVias() == null || st.getHeadsignVias().isEmpty())
    ) {
      return null;
    }

    String[][] vias = new String[stopTimes.size()][];

    int i = 0;
    for (final StopTime st : stopTimes) {
      if (st.getHeadsignVias() == null) {
        vias[i] = EMPTY_STRING_ARRAY;
        i++;
        continue;
      }

      vias[i] = st.getHeadsignVias().toArray(EMPTY_STRING_ARRAY);
      i++;
    }

    return vias;
  }
}
