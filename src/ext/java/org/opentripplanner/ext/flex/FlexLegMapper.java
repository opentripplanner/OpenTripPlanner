package org.opentripplanner.ext.flex;

import java.util.ArrayList;
import java.util.Locale;
import org.opentripplanner.ext.flex.edgetype.FlexTripEdge;
import org.opentripplanner.ext.flex.template.FlexAccessEgressTemplate;
import org.opentripplanner.ext.flex.trip.FlexTrip;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.Place;
import org.opentripplanner.routing.algorithm.mapping.GraphPathToItineraryMapper;

public class FlexLegMapper {

  static public void fixFlexTripLeg(Leg leg, FlexTripEdge flexTripEdge) {
      FlexTrip flexTrip = flexTripEdge.getFlexTrip();
      FlexAccessEgressTemplate flexTemplate = flexTripEdge.flexTemplate;

      leg.setIntermediateStops(new ArrayList<>());
      leg.setDistanceMeters(flexTripEdge.getDistanceMeters());

      leg.setServiceDate(flexTemplate.serviceDate);
      leg.setHeadsign(flexTrip.getTrip().getTripHeadsign());
      leg.setWalkSteps(new ArrayList<>());

      leg.setBoardRule(flexTrip.getBoardRule(flexTemplate.fromStopIndex));
      leg.setAlightRule(flexTrip.getAlightRule(flexTemplate.toStopIndex));

      leg.setBoardStopPosInPattern(flexTemplate.fromStopIndex);
      leg.setAlightStopPosInPattern(flexTemplate.toStopIndex);

      leg.setDropOffBookingInfo(flexTrip.getDropOffBookingInfo(leg.getBoardStopPosInPattern()));
      leg.setPickupBookingInfo(flexTrip.getPickupBookingInfo(leg.getAlightStopPosInPattern()));
  }

    public static void addFlexPlaces(Leg leg, FlexTripEdge flexEdge) {
        leg.setFrom(Place.forFlexStop(flexEdge.s1, flexEdge.getFromVertex()));
        leg.setTo(Place.forFlexStop(flexEdge.s2, flexEdge.getToVertex()));
    }
}
