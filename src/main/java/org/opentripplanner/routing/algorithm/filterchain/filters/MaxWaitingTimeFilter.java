package org.opentripplanner.routing.algorithm.filterchain.filters;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

public class MaxWaitingTimeFilter implements ItineraryFilter {

  private int maxWaitingTime;
  private String name;

  public MaxWaitingTimeFilter(String name, int maxWaitingTime) {
    super();
    this.maxWaitingTime = maxWaitingTime;
    this.name = name;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public List<Itinerary> filter(final List<Itinerary> itineraries) {
    return itineraries.stream().filter(it -> it.waitingTimeSeconds <= maxWaitingTime)
        .collect(Collectors.toList());
  }

  @Override
  public boolean removeItineraries() {
    return true;
  }
}
