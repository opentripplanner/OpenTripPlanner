package org.opentripplanner.model.plan.grouppriority;

import java.util.Collection;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.raptor.api.request.RaptorTransitGroupPriorityCalculator;
import org.opentripplanner.transit.model.network.grouppriority.DefaultTransitGroupPriorityCalculator;
import org.opentripplanner.transit.model.network.grouppriority.TransitGroupPriorityService;

/**
 * This class will set the {@link Itinerary#getGeneralizedCost2()} value if the feature is
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

  public void decorate(Collection<Itinerary> itineraries) {
    if (!priorityGroupConfigurator.isEnabled()) {
      return;
    }
    for (Itinerary it : itineraries) {
      decorate(it);
    }
  }

  public void decorate(Itinerary itinerary) {
    if (itinerary.getGeneralizedCost2().isEmpty() && priorityGroupConfigurator.isEnabled()) {
      int c2 = priorityGroupConfigurator.baseGroupId();
      for (Leg leg : itinerary.getLegs()) {
        if (leg.getTrip() != null) {
          int newGroupId = priorityGroupConfigurator.lookupTransitGroupPriorityId(leg.getTrip());
          c2 = transitGroupCalculator.mergeInGroupId(c2, newGroupId);
        }
      }
      itinerary.setGeneralizedCost2(c2);
    }
  }
}
