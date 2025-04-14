package org.opentripplanner.model.plan.grouppriority;

import java.util.ArrayList;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupPriorityCalculator;
import org.opentripplanner.transit.model.network.grouppriority.DefaultTransitGroupPriorityCalculator;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;

/**
 * This class will set the {@link Itinerary#generalizedCost2()} value if the feature is
 * enabled and no such value is set. The AStar router does not produce itineraries with this,
 * so we decorate itineraries with this here to make sure the `c2` is set correct and can be
 * used in the itinerary-filter-chain.
 */
public class TransitGroupPriorityItineraryDecorator {

  private final TransitGroupPriorityService priorityGroupConfigurator;
  private final RaptorTransitGroupPriorityCalculator transitGroupCalculator;

  public TransitGroupPriorityItineraryDecorator(
    TransitGroupPriorityService priorityGroupConfigurator
  ) {
    this.priorityGroupConfigurator = priorityGroupConfigurator;
    this.transitGroupCalculator = new DefaultTransitGroupPriorityCalculator();
  }

  public List<Itinerary> decorate(List<Itinerary> itineraries) {
    if (!priorityGroupConfigurator.isEnabled() || isC2SetForAllItineraries(itineraries)) {
      return itineraries;
    }
    var list = new ArrayList<Itinerary>();
    for (Itinerary it : itineraries) {
      list.add(decorate(it));
    }
    return list;
  }

  private Itinerary decorate(Itinerary itinerary) {
    if (!itinerary.generalizedCost2().isEmpty()) {
      return itinerary;
    }
    int c2 = priorityGroupConfigurator.baseGroupId();
    for (Leg leg : itinerary.legs()) {
      if (leg.trip() != null) {
        int newGroupId = priorityGroupConfigurator.lookupTransitGroupPriorityId(leg.trip());
        c2 = transitGroupCalculator.mergeInGroupId(c2, newGroupId);
      }
    }
    return itinerary.copyOf().withGeneralizedCost2(c2).build();
  }

  private static boolean isC2SetForAllItineraries(List<Itinerary> itineraries) {
    return itineraries.stream().allMatch(it -> it.generalizedCost2().isPresent());
  }
}
