package org.opentripplanner.ext.flex.trip;

import org.opentripplanner.ext.flex.FlexServiceDate;
import org.opentripplanner.ext.flex.flexpathcalculator.FlexPathCalculator;
import org.opentripplanner.ext.flex.template.FlexAccessTemplate;
import org.opentripplanner.ext.flex.template.FlexEgressTemplate;
import org.opentripplanner.model.StopLocation;
import org.opentripplanner.model.StopTime;
import org.opentripplanner.model.Trip;
import org.opentripplanner.routing.graphfinder.NearbyStop;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.opentripplanner.model.StopPattern.PICKDROP_NONE;

public class ContinuousPickupDropOffTrip extends FlexTrip<Double> {

  public ContinuousPickupDropOffTrip(Trip trip, List<StopTime> stopTimes) {super(trip);}

  public static boolean hasContinuousStops(List<StopTime> stopTimes) {
    return stopTimes
        .stream()
        .anyMatch(st -> st.getFlexContinuousPickup() != PICKDROP_NONE || st.getFlexContinuousDropOff() != PICKDROP_NONE);
  }

  @Override
  public Stream<FlexAccessTemplate<Double>> getFlexAccessTemplates(
      NearbyStop access, FlexServiceDate date, FlexPathCalculator<Integer> calculator
  ) {
    return Stream.empty();
  }

  @Override
  public Stream<FlexEgressTemplate<Double>> getFlexEgressTemplates(
      NearbyStop egress, FlexServiceDate date, FlexPathCalculator<Integer> calculator
  ) {
    return Stream.empty();
  }

  @Override
  public int earliestDepartureTime(
      int departureTime, Double fromStopIndex, Double toStopIndex, int flexTime
  ) {
    return departureTime;
  }

  @Override
  public int latestArrivalTime(
      int arrivalTime, Double fromStopIndex, Double toStopIndex, int flexTime
  ) {
    return arrivalTime;
  }

  @Override
  public Collection<StopLocation> getStops() {
    return Collections.EMPTY_LIST;
  }
}
