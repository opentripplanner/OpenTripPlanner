package org.opentripplanner.ext.fares.impl.gtfs;

import org.opentripplanner.ext.fares.model.FareDistance;
import org.opentripplanner.ext.fares.model.FareLegRule;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.transit.model.basic.Distance;

public class DistanceMatcher {

  static boolean matchesDistance(TransitLeg leg, FareLegRule rule) {
    // If no valid distance type is given, do not consider distances in fare computation
    FareDistance distance = rule.fareDistance();
    if (distance instanceof FareDistance.Stops(int min, int max)) {
      var numStops = leg.listIntermediateStops().size();
      return numStops >= min && max >= numStops;
    } else if (
      rule.fareDistance() instanceof FareDistance.LinearDistance(Distance min, Distance max)
    ) {
      var legDistance = leg.from().coordinate.distanceTo(leg.to().coordinate);

      return legDistance > min.toMeters() && legDistance < max.toMeters();
    } else return true;
  }
}
