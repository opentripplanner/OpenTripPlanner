package org.opentripplanner.ext.flex;

import java.util.ArrayList;
import java.util.Locale;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;

public class FlexLegMapper {

  static public void fixFlexTripLeg(Leg leg, FlexTripEdge flexTripEdge) {
      leg.setIntermediateStops(new ArrayList<>());
      leg.setDistanceMeters(flexTripEdge.getDistanceMeters());

      leg.setServiceDate(flexTripEdge.flexTemplate.serviceDate);
      leg.setHeadsign(flexTripEdge.getTrip().getTripHeadsign());
      leg.setWalkSteps(new ArrayList<>());

      leg.setBoardRule(GraphPathToItineraryMapper.getBoardAlightMessage(2));
      leg.setAlightRule(GraphPathToItineraryMapper.getBoardAlightMessage(3));

      leg.setBoardStopPosInPattern(flexTripEdge.flexTemplate.fromStopIndex);
      leg.setAlightStopPosInPattern(flexTripEdge.flexTemplate.toStopIndex);

      leg.setDropOffBookingInfo(
              flexTripEdge.getFlexTrip().getDropOffBookingInfo(leg.getBoardStopPosInPattern()));
      leg.setPickupBookingInfo(
              flexTripEdge.getFlexTrip().getPickupBookingInfo(leg.getAlightStopPosInPattern()));
  }

    public static void addFlexPlaces(Leg leg, FlexTripEdge flexEdge, Locale requestedLocale) {
        leg.setFrom(Place.forFlexStop(flexEdge.s1, flexEdge.getFromVertex()));
        leg.setTo(Place.forFlexStop(flexEdge.s2, flexEdge.getToVertex()));
    }
}
