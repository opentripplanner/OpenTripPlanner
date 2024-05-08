package org.opentripplanner.ext.flex.template;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.stream.IntStream;
import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.flexpathcalculator.ScheduledFlexPathCalculator;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.ext.flex.trip.ScheduledDeviatedTrip;
import org.opentripplanner.ext.flex.trip.UnscheduledTrip;
import org.opentripplanner.routing.graphfinder.NearbyStop;
import org.opentripplanner.standalone.config.sandbox.FlexConfig;
import org.opentripplanner.transit.model.site.GroupStop;
import org.opentripplanner.transit.model.site.StopLocation;

public class FlexTemplateFactory {

  private final FlexPathCalculator calculator;
  private final FlexConfig config;
  private FlexTrip trip;
  private NearbyStop nearbyStop;
  private FlexServiceDate date;

  public FlexTemplateFactory(FlexPathCalculator calculator, FlexConfig config) {
    this.calculator = calculator;
    this.config = config;
  }

  private void with(FlexTrip trip, NearbyStop nearbyStop, FlexServiceDate date) {
    this.trip = trip;
    this.nearbyStop = nearbyStop;
    this.date = date;
  }

  public List<FlexAccessTemplate> createAccessTemplates(
    FlexTrip flexTrip,
    NearbyStop access,
    FlexServiceDate date
  ) {
    // TODO: Merge the type specific methods and push differences into the types,
    //       The diff is likely to be important business rules.
    if (flexTrip instanceof ScheduledDeviatedTrip sdt) {
      return createAccessTemplatesForScheduledDeviatedTrip(sdt, access, date);
    } else if (flexTrip instanceof UnscheduledTrip ust) {
      return createAccessTemplatesForUnscheduledTrip(ust, access, date);
    }
    throw new IllegalArgumentException("Unknown type: " + flexTrip.getClass());
  }

  public List<FlexEgressTemplate> createEgressTemplates(
    FlexTrip flexTrip,
    NearbyStop egress,
    FlexServiceDate date
  ) {
    // TODO: Merge the type specific methods and push differences into the types,
    //       The diff is likely to be important business rules.
    if (flexTrip instanceof ScheduledDeviatedTrip sdt) {
      return createEgressTemplatesForScheduledDeviatedTrip(sdt, egress, date);
    } else if (flexTrip instanceof UnscheduledTrip ust) {
      return createEgressTemplatesForUnscheduledTrip(ust, egress, date);
    }
    throw new IllegalArgumentException("Unknown type: " + flexTrip.getClass());
  }

  private List<FlexAccessTemplate> createAccessTemplatesForScheduledDeviatedTrip(
    ScheduledDeviatedTrip flexTrip,
    NearbyStop access,
    FlexServiceDate date
  ) {
    with(flexTrip, access, date);
    var scheduledCalculator = new ScheduledFlexPathCalculator(calculator, flexTrip);

    int fromIndex = flexTrip.findBoardIndex(access.stop);

    if (fromIndex == FlexTrip.STOP_INDEX_NOT_FOUND) {
      return List.of();
    }

    ArrayList<FlexAccessTemplate> res = new ArrayList<>();

    for (int toIndex = fromIndex; toIndex < flexTrip.numberOfStops(); toIndex++) {
      if (flexTrip.getAlightRule(toIndex).isNotRoutable()) {
        continue;
      }
      for (IndexedStopLocation stop : expandStopsAt(flexTrip, toIndex)) {
        res.add(
          new FlexAccessTemplate(
            access,
            trip,
            fromIndex,
            toIndex,
            stop.stop,
            date,
            scheduledCalculator,
            config
          )
        );
      }
    }
    return res;
  }

  private List<FlexEgressTemplate> createEgressTemplatesForScheduledDeviatedTrip(
    ScheduledDeviatedTrip flexTrip,
    NearbyStop egress,
    FlexServiceDate date
  ) {
    with(flexTrip, egress, date);

    FlexPathCalculator scheduledCalculator = new ScheduledFlexPathCalculator(calculator, flexTrip);

    var toIndex = flexTrip.findAlightIndex(egress.stop);

    if (toIndex == FlexTrip.STOP_INDEX_NOT_FOUND) {
      return List.of();
    }

    List<FlexEgressTemplate> res = new ArrayList<>();

    for (int fromIndex = toIndex; fromIndex >= 0; fromIndex--) {
      if (flexTrip.getBoardRule(fromIndex).isNotRoutable()) {
        continue;
      }
      for (var stop : expandStopsAt(flexTrip, fromIndex)) {
        res.add(
          new FlexEgressTemplate(
            egress,
            flexTrip,
            fromIndex,
            toIndex,
            stop.stop,
            date,
            scheduledCalculator,
            config
          )
        );
      }
    }
    return res;
  }

  private List<FlexAccessTemplate> createAccessTemplatesForUnscheduledTrip(
    UnscheduledTrip flexTrip,
    NearbyStop access,
    FlexServiceDate date
  ) {
    with(flexTrip, access, date);

    // Find boarding index, also check if it's boardable
    final int fromIndex = flexTrip.findBoardIndex(access.stop);

    if (fromIndex == FlexTrip.STOP_INDEX_NOT_FOUND) {
      return List.of();
    }

    // templates will be generated from the boardingIndex to the end of the trip
    final int lastIndexInTrip = flexTrip.numberOfStops() - 1;

    // Check if trip is possible
    IntStream indices;
    if (flexTrip.numberOfStops() == 1) {
      indices = IntStream.of(fromIndex);
    } else {
      indices = IntStream.range(fromIndex + 1, lastIndexInTrip + 1);
    }

    for (int toIndex = fromIndex; toIndex < flexTrip.numberOfStops(); toIndex++) {
      if (flexTrip.getAlightRule(toIndex).isNotRoutable()) {
        continue;
      }
      for (var stop : expandStopsAt(flexTrip, toIndex)) {}
    }

    // check for every stop after fromIndex if you can alight, if so return a template
    return indices
      // if you cannot alight at an index, the trip is not possible
      .filter(alightIndex -> flexTrip.getAlightRule(alightIndex).isRoutable())
      // expand GroupStops and build IndexedStopLocations
      .mapToObj(alightIndex -> expandStopsAt(flexTrip, alightIndex).stream())
      // flatten stream of streams
      .flatMap(Function.identity())
      // create template
      .map(alightStop ->
        new FlexAccessTemplate(
          access,
          flexTrip,
          fromIndex,
          alightStop.index,
          alightStop.stop,
          date,
          calculator,
          config
        )
      )
      .toList();
  }

  private List<FlexEgressTemplate> createEgressTemplatesForUnscheduledTrip(
    UnscheduledTrip flexTrip,
    NearbyStop egress,
    FlexServiceDate date
  ) {
    // templates will be generated from the first index to the toIndex
    int firstIndexInTrip = 0;

    // Find alighting index, also check if alighting is allowed
    int toIndex = flexTrip.findAlightIndex(egress.stop);

    // Check if trip is possible
    if (toIndex == FlexTrip.STOP_INDEX_NOT_FOUND) {
      return List.of();
    }

    IntStream indices;
    if (flexTrip.numberOfStops() == 1) {
      indices = IntStream.of(toIndex);
    } else {
      indices = IntStream.range(firstIndexInTrip, toIndex + 1);
    }
    // check for every stop after fromIndex if you can alight, if so return a template
    return indices
      // if you cannot board at this index, the trip is not possible
      .filter(boardIndex -> flexTrip.getBoardRule(boardIndex).isRoutable())
      // expand GroupStops and build IndexedStopLocations
      .mapToObj(boardIndex -> expandStopsAt(flexTrip, boardIndex).stream())
      // flatten stream of streams
      .flatMap(Function.identity())
      // create template
      .map(boardStop ->
        new FlexEgressTemplate(
          egress,
          flexTrip,
          boardStop.index,
          toIndex,
          boardStop.stop,
          date,
          calculator,
          config
        )
      )
      .toList();
  }

  private static List<IndexedStopLocation> expandStopsAt(FlexTrip<?, ?> flexTrip, int index) {
    var stop = flexTrip.getStop(index);
    return stop instanceof GroupStop groupStop
      ? groupStop.getChildLocations().stream().map(s -> new IndexedStopLocation(index, s)).toList()
      : List.of(new IndexedStopLocation(index, stop));
  }

  private record IndexedStopLocation(int index, StopLocation stop) {}
}
