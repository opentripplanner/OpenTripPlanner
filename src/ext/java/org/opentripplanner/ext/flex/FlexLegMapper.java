package org.opentripplanner.ext.flex;

import java.util.ArrayList;
import java.util.Locale;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;

public class FlexLegMapper {

  static public void fixFlexTripLeg(Leg leg, FlexTripEdge flexTripEdge) {
      leg.intermediateStops = new ArrayList<>();
      leg.distanceMeters = flexTripEdge.getDistanceMeters();

      leg.serviceDate = flexTripEdge.flexTemplate.serviceDate;
      leg.headsign = flexTripEdge.getTrip().getTripHeadsign();
      leg.walkSteps = new ArrayList<>();

      leg.boardRule = GraphPathToItineraryMapper.getBoardAlightMessage(2);
      leg.alightRule = GraphPathToItineraryMapper.getBoardAlightMessage(3);

      leg.dropOffBookingInfo = flexTripEdge.getFlexTrip().getDropOffBookingInfo(leg.from.stopIndex);
      leg.pickupBookingInfo = flexTripEdge.getFlexTrip().getPickupBookingInfo(leg.from.stopIndex);
  }

    public static void addFlexPlaces(Leg leg, FlexTripEdge flexEdge, Locale requestedLocale) {
        leg.from = Place.forFlexStop(
                flexEdge.s1,
                flexEdge.getFromVertex(),
                flexEdge.flexTemplate.fromStopIndex,
                null,
                requestedLocale
        );
        leg.to = Place.forFlexStop(
                flexEdge.s2,
                flexEdge.getToVertex(),
                flexEdge.flexTemplate.toStopIndex,
                null,
                requestedLocale
        );
    }
}
