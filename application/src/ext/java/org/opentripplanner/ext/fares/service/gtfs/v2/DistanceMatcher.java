package org.opentripplanner.ext.fares.service.gtfs.v2;

import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.plan.TransitLeg;

class DistanceMatcher {

  static boolean matchesDistance(TransitLeg leg, FareLegRule rule) {
    // If no valid distance type is given, do not consider distances in fare computation
    FareDistance distance = rule.fareDistance();
    if (distance instanceof FareDistance.Stops(int min, int max)) {
      // the board stop does not contribute towards the final count, but the alight stop does
      // therefore we add 1
      var numStops = leg.listIntermediateStops().size() + 1;
      return numStops >= min && max >= numStops;
    } else {
      return true;
    }
  }
}
