package org.opentripplanner.ext.fares;

import org.opentripplanner.ext.fares.model.FareProductInstance;
import org.opentripplanner.framework.collection.ListUtils;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.core.ItineraryFares;

/**
 * Takes fares and applies them to the legs of an itinerary.
 */
public class FaresToItineraryMapper {

  public static void addFaresToLegs(ItineraryFares fares, Itinerary i) {
    var itineraryInstances = fares
      .getItineraryProducts()
      .stream()
      .map(fp -> {
        var instanceId = fp.uniqueInstanceId(i.firstLeg().getStartTime());
        return new FareProductInstance(instanceId, fp);
      })
      .toList();

    i
      .getLegs()
      .forEach(l -> {
        var legInstances = fares.getLegProducts().get(l);
        l.addFareProducts(ListUtils.combine(itineraryInstances, legInstances));
      });
  }
}
