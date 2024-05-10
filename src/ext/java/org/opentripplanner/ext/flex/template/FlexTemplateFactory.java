package org.opentripplanner.ext.flex.template;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.ScheduledFlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * The factory is used to create flex trip templates.
 */
public class FlexTemplateFactory {

  private final FlexPathCalculator calculator;
  private final Duration maxTransferDuration;
  private FlexServiceDate date;
  private FlexTrip<?, ?> trip;
  private NearbyStop nearbyStop;

  private FlexTemplateFactory(FlexPathCalculator calculator, Duration maxTransferDuration) {
    this.calculator = Objects.requireNonNull(calculator);
    this.maxTransferDuration = Objects.requireNonNull(maxTransferDuration);
  }

  public static FlexTemplateFactory of(
    FlexPathCalculator calculator,
    Duration maxTransferDuration
  ) {
    return new FlexTemplateFactory(calculator, maxTransferDuration);
  }

  /**
   * Add required parameters to the factory before calling the create methods.
   */
  public FlexTemplateFactory with(
    FlexServiceDate date,
    FlexTrip<?, ?> flexTrip,
    NearbyStop nearbyStop
  ) {
    this.date = Objects.requireNonNull(date);
    this.trip = Objects.requireNonNull(flexTrip);
    this.nearbyStop = Objects.requireNonNull(nearbyStop);
    return this;
  }

  public List<FlexAccessTemplate> createAccessTemplates() {
    assertRequiredParametersSet();

    int boardIndex = trip.findBoardIndex(stop());

    if (boardIndex == FlexTrip.STOP_INDEX_NOT_FOUND) {
      return List.of();
    }

    ArrayList<FlexAccessTemplate> result = new ArrayList<>();

    int alightIndex = isBoardingAndAlightingAtSameStopPositionAllowed()
      ? boardIndex
      : boardIndex + 1;

    for (; alightIndex < trip.numberOfStops(); alightIndex++) {
      if (trip.getAlightRule(alightIndex).isRoutable()) {
        for (var stop : expandStopsAt(trip, alightIndex)) {
          result.add(createAccessTemplate(trip, boardIndex, stop, alightIndex));
        }
      }
    }
    return result;
  }

  public List<FlexEgressTemplate> createEgressTemplates() {
    assertRequiredParametersSet();

    var alightIndex = trip.findAlightIndex(stop());

    if (alightIndex == FlexTrip.STOP_INDEX_NOT_FOUND) {
      return List.of();
    }

    List<FlexEgressTemplate> result = new ArrayList<>();

    int end = isBoardingAndAlightingAtSameStopPositionAllowed() ? alightIndex : alightIndex - 1;

    for (int boardIndex = 0; boardIndex <= end; boardIndex++) {
      if (trip.getBoardRule(boardIndex).isRoutable()) {
        for (var stop : expandStopsAt(trip, boardIndex)) {
          result.add(createEgressTemplate(trip, stop, boardIndex, alightIndex));
        }
      }
    }
    return result;
  }

  private void assertRequiredParametersSet() {
    Objects.requireNonNull(date);
    Objects.requireNonNull(trip);
    Objects.requireNonNull(nearbyStop);
  }

  /**
   * With respect to one journey/itinerary this method retuns {@code true} if a passenger can
   * board and alight at the same stop in the journey pattern. This is not allowed for regular
   * stops, but it would make sense to allow it for area stops or group stops.
   * <p>
   * In NeTEx this is not allowed.
   * <p>
   * In GTFS this is no longer allowed according to specification. But it was allowed earlier.
   * <p>
   * This method simply returns {@code false}, but we keep it here for documentation. If requested,
   * we can add code to be backward compatible with the old GTFS version here.
   */
  private boolean isBoardingAndAlightingAtSameStopPositionAllowed() {
    return false;
  }

  /**
   * Return the access/egress stop, can be Regular-, Group- and AreaStop.
   * Se also {@link #expandStopsAt(FlexTrip, int)}.
   */
  private StopLocation stop() {
    return nearbyStop.stop;
  }

  private static List<StopLocation> expandStopsAt(FlexTrip<?, ?> flexTrip, int index) {
    var stop = flexTrip.getStop(index);
    return stop instanceof GroupStop groupStop ? groupStop.getChildLocations() : List.of(stop);
  }

  FlexPathCalculator createCalculator(FlexTrip<?, ?> flexTrip) {
    return flexTrip instanceof ScheduledDeviatedTrip
      ? new ScheduledFlexPathCalculator(calculator, flexTrip)
      : calculator;
  }

  private FlexAccessTemplate createAccessTemplate(
    FlexTrip<?, ?> flexTrip,
    int boardIndex,
    StopLocation alightStop,
    int alightStopIndex
  ) {
    return new FlexAccessTemplate(
      nearbyStop,
      flexTrip,
      boardIndex,
      alightStopIndex,
      alightStop,
      date,
      createCalculator(flexTrip),
      maxTransferDuration
    );
  }

  private FlexEgressTemplate createEgressTemplate(
    FlexTrip<?, ?> flexTrip,
    StopLocation boardStop,
    int boardStopIndex,
    int alightIndex
  ) {
    return new FlexEgressTemplate(
      nearbyStop,
      flexTrip,
      boardStopIndex,
      alightIndex,
      boardStop,
      date,
      createCalculator(flexTrip),
      maxTransferDuration
    );
  }
}
