package org.opentripplanner.ext.flex.trip;

import static org.opentripplanner.model.PickDrop.NONE;
import static org.opentripplanner.model.StopTime.MISSING_VALUE;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.framework.lang.IntRange;
import org.opentripplanner.model.BookingInfo;
import org.opentripplanner.model.PickDrop;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.transit.model.framework.FeedScopedId;
import org.opentripplanner.transit.model.framework.TransitBuilder;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * This type of FlexTrip is used when a taxi-type service is modeled, which operates in any number
 * of areas/groups of stops without a set schedule. The travel times are calculated based
 * on the driving time between the stops, with the schedule times being used just for deciding if a
 * trip is possible.
 * <p>
 * An unscheduled flex trip may visit/drive from one flex stops(areas/group of stop locations) to
 * any other stop in the pattern without driving through the stops in between. Only the times in the
 * two stops used need to match the path.
 * <p>
 * For a discussion of this behaviour see https://github.com/MobilityData/gtfs-flex/issues/76
 */
public class UnscheduledTrip extends FlexTrip<UnscheduledTrip, UnscheduledTripBuilder> {

  private static final Set<Integer> N_STOPS = Set.of(1, 2);
  private static final int INDEX_NOT_FOUND = -1;

  private final StopTimeWindow[] stopTimes;

  private final BookingInfo[] dropOffBookingInfos;
  private final BookingInfo[] pickupBookingInfos;

  public UnscheduledTrip(UnscheduledTripBuilder builder) {
    super(builder);
    List<StopTime> stopTimes = builder.stopTimes();
    if (!isUnscheduledTrip(stopTimes)) {
      throw new IllegalArgumentException("Incompatible stopTimes for unscheduled trip");
    }

    var size = stopTimes.size();
    this.stopTimes = new StopTimeWindow[size];
    this.dropOffBookingInfos = new BookingInfo[size];
    this.pickupBookingInfos = new BookingInfo[size];

    for (int i = 0; i < size; i++) {
      this.stopTimes[i] = new StopTimeWindow(stopTimes.get(i));
      this.dropOffBookingInfos[i] = stopTimes.get(0).getDropOffBookingInfo();
      this.pickupBookingInfos[i] = stopTimes.get(0).getPickupBookingInfo();
    }
  }

  public static UnscheduledTripBuilder of(FeedScopedId id) {
    return new UnscheduledTripBuilder(id);
  }

  /**
   * Tests if the stop times constitute an {@link UnscheduledTrip}.
   * <p>
   * Returns true for the following cases:
   *  - A single fixed scheduled stop followed by a flexible one
   *  - One or more stop times with a flexible time window but no fixed stop in between them
   */
  public static boolean isUnscheduledTrip(List<StopTime> stopTimes) {
    Predicate<StopTime> hasFlexWindow = st ->
      st.getFlexWindowStart() != MISSING_VALUE || st.getFlexWindowEnd() != MISSING_VALUE;
    Predicate<StopTime> hasContinuousStops = stopTime ->
      stopTime.getFlexContinuousDropOff() != NONE || stopTime.getFlexContinuousPickup() != NONE;
    if (stopTimes.isEmpty()) {
      return false;
    } else if (stopTimes.stream().anyMatch(hasContinuousStops)) {
      return false;
    } else if (N_STOPS.contains(stopTimes.size())) {
      return stopTimes.stream().anyMatch(hasFlexWindow);
    } else {
      return stopTimes.stream().allMatch(hasFlexWindow);
    }
  }

  @Override
  public Stream<FlexAccessTemplate> getFlexAccessTemplates(
    NearbyStop access,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  ) {
    // Find boarding index, also check if it's boardable
    final int fromIndex = getFromIndex(access);

    // templates will be generated from the boardingIndex to the end of the trip
    final int lastIndexInTrip = stopTimes.length - 1;

    // Check if trip is possible
    if (fromIndex == INDEX_NOT_FOUND || fromIndex > lastIndexInTrip) {
      return Stream.empty();
    }

    IntStream indices;
    if (stopTimes.length == 1) {
      indices = IntStream.of(fromIndex);
    } else {
      indices = IntStream.range(fromIndex + 1, lastIndexInTrip + 1);
    }
    // check for every stop after fromIndex if you can alight, if so return a template
    return indices
      // if you cannot alight at an index, the trip is not possible
      .filter(alightIndex -> getAlightRule(alightIndex).isRoutable())
      // expand GroupStops and build IndexedStopLocations
      .mapToObj(this::expandStops)
      // flatten stream of streams
      .flatMap(Function.identity())
      // create template
      .map(alightStop ->
        new FlexAccessTemplate(
          access,
          this,
          fromIndex,
          alightStop.index,
          alightStop.stop,
          date,
          calculator,
          config
        )
      );
  }

  @Override
  public Stream<FlexEgressTemplate> getFlexEgressTemplates(
    NearbyStop egress,
    FlexServiceDate date,
    FlexPathCalculator calculator,
    FlexConfig config
  ) {
    // templates will be generated from the first index to the toIndex
    int firstIndexInTrip = 0;

    // Find alighting index, also check if alighting is allowed
    int toIndex = getToIndex(egress);

    // Check if trip is possible
    if (toIndex == INDEX_NOT_FOUND || firstIndexInTrip > toIndex) {
      return Stream.empty();
    }

    IntStream indices;
    if (stopTimes.length == 1) {
      indices = IntStream.of(toIndex);
    } else {
      indices = IntStream.range(firstIndexInTrip, toIndex + 1);
    }
    // check for every stop after fromIndex if you can alight, if so return a template
    return indices
      // if you cannot board at this index, the trip is not possible
      .filter(boardIndex -> getBoardRule(boardIndex).isRoutable())
      // expand GroupStops and build IndexedStopLocations
      .mapToObj(this::expandStops)
      // flatten stream of streams
      .flatMap(Function.identity())
      // create template
      .map(boardStop ->
        new FlexEgressTemplate(
          egress,
          this,
          boardStop.index,
          toIndex,
          boardStop.stop,
          date,
          calculator,
          config
        )
      );
  }

  @Override
  public int earliestDepartureTime(
    int requestedDepartureTime,
    int fromStopIndex,
    int toStopIndex,
    int tripDurationSeconds
  ) {
    var optionalDepartureTimeWindow = departureTimeWindow(
      fromStopIndex,
      toStopIndex,
      tripDurationSeconds
    );

    if (optionalDepartureTimeWindow.isEmpty()) {
      return MISSING_VALUE;
    }
    var win = optionalDepartureTimeWindow.get();
    if (win.endInclusive() < requestedDepartureTime) {
      return MISSING_VALUE;
    }
    return Math.max(requestedDepartureTime, win.startInclusive());
  }

  @Override
  public int earliestDepartureTime(int stopIndex) {
    return stopTimes[stopIndex].start();
  }

  @Override
  public int latestArrivalTime(
    int requestedArrivalTime,
    int fromStopIndex,
    int toStopIndex,
    int tripDurationSeconds
  ) {
    var optionalArrivalTimeWindow = arrivalTimeWindow(
      fromStopIndex,
      toStopIndex,
      tripDurationSeconds
    );

    if (optionalArrivalTimeWindow.isEmpty()) {
      return MISSING_VALUE;
    }
    var win = optionalArrivalTimeWindow.get();
    if (win.startInclusive() > requestedArrivalTime) {
      return MISSING_VALUE;
    }
    return Math.min(requestedArrivalTime, win.endInclusive());
  }

  @Override
  public int latestArrivalTime(int stopIndex) {
    return stopTimes[stopIndex].end();
  }

  @Override
  public Set<StopLocation> getStops() {
    return Arrays.stream(stopTimes).map(StopTimeWindow::stop).collect(Collectors.toSet());
  }

  @Override
  public BookingInfo getDropOffBookingInfo(int i) {
    return dropOffBookingInfos[i];
  }

  @Override
  public BookingInfo getPickupBookingInfo(int i) {
    return pickupBookingInfos[i];
  }

  @Override
  public PickDrop getBoardRule(int i) {
    return stopTimes[i].pickupType();
  }

  @Override
  public PickDrop getAlightRule(int i) {
    return stopTimes[i].dropOffType();
  }

  @Override
  public boolean isBoardingPossible(NearbyStop stop) {
    return getFromIndex(stop) != INDEX_NOT_FOUND;
  }

  @Override
  public boolean isAlightingPossible(NearbyStop stop) {
    return getToIndex(stop) != INDEX_NOT_FOUND;
  }

  @Override
  public boolean sameAs(@Nonnull UnscheduledTrip other) {
    return (
      super.sameAs(other) &&
      Arrays.equals(stopTimes, other.stopTimes) &&
      Arrays.equals(pickupBookingInfos, other.pickupBookingInfos) &&
      Arrays.equals(dropOffBookingInfos, other.dropOffBookingInfos)
    );
  }

  @Nonnull
  @Override
  public TransitBuilder<UnscheduledTrip, UnscheduledTripBuilder> copy() {
    return new UnscheduledTripBuilder(this);
  }

  private Stream<IndexedStopLocation> expandStops(int index) {
    var stop = stopTimes[index].stop();
    return stop instanceof GroupStop groupStop
      ? groupStop.getChildLocations().stream().map(s -> new IndexedStopLocation(index, s))
      : Stream.of(new IndexedStopLocation(index, stop));
  }

  private int getFromIndex(NearbyStop accessEgress) {
    for (int i = 0; i < stopTimes.length; i++) {
      if (getBoardRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop();
      if (stop instanceof GroupStop groupStop) {
        if (groupStop.getChildLocations().contains(accessEgress.stop)) {
          return i;
        }
      } else {
        if (stop.equals(accessEgress.stop)) {
          return i;
        }
      }
    }
    return INDEX_NOT_FOUND;
  }

  private int getToIndex(NearbyStop accessEgress) {
    for (int i = stopTimes.length - 1; i >= 0; i--) {
      if (getAlightRule(i).isNotRoutable()) {
        continue;
      }
      StopLocation stop = stopTimes[i].stop();
      if (stop instanceof GroupStop groupStop) {
        if (groupStop.getChildLocations().contains(accessEgress.stop)) {
          return i;
        }
      } else {
        if (stop.equals(accessEgress.stop)) {
          return i;
        }
      }
    }
    return INDEX_NOT_FOUND;
  }

  private Optional<IntRange> departureTimeWindow(
    int fromStopIndex,
    int toStopIndex,
    int tripDurationSeconds
  ) {
    // Align the from and to time-windows by subtracting the trip-duration from the to-time-window.
    var fromTime = stopTimes[fromStopIndex].timeWindow();
    var toTimeShifted = stopTimes[toStopIndex].timeWindow().minus(tripDurationSeconds);

    // Then take the intersection of the aligned windows to find the window where the
    // requested-departure-time must be within
    return fromTime.intersect(toTimeShifted);
  }

  private Optional<IntRange> arrivalTimeWindow(
    int fromStopIndex,
    int toStopIndex,
    int tripDurationSeconds
  ) {
    // Align the from and to time-windows by adding the trip-duration to the from-time-window.
    var fromTimeShifted = stopTimes[fromStopIndex].timeWindow().plus(tripDurationSeconds);
    var toTime = stopTimes[toStopIndex].timeWindow();

    // Then take the intersection of the aligned windows to find the window where the
    // requested-arrival-time must be within
    return toTime.intersect(fromTimeShifted);
  }

  private record IndexedStopLocation(int index, StopLocation stop) {}
}
