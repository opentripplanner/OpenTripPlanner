package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DebugFilterWrapper implements ItineraryFilter {

  private final ItineraryFilter delegate;
  private final List<Itinerary> deletedItineraries;

  public DebugFilterWrapper(ItineraryFilter delegate, List<Itinerary> deletedItineraries) {
    this.delegate = delegate;
    this.deletedItineraries = deletedItineraries;
  }

  @Override
  public String name() {
    return delegate.name();
  }

  @Override
  public List<Itinerary> filter(List<Itinerary> itineraries) {
    List<Itinerary> previouslyUnfiltered = remindingItineraries(itineraries);
    List<Itinerary> filtered = delegate.filter(previouslyUnfiltered);

    for (Itinerary it : previouslyUnfiltered) {
      if(!filtered.contains(it)) {
        markItineraryAsDeleted(it);
      }
    }
    return itineraries;
  }

  private List<Itinerary> remindingItineraries(List<Itinerary> originalList) {
    return originalList.stream()
        .filter(it -> !deletedItineraries.contains(it))
        .collect(Collectors.toList());
  }


  /* inner classes */

  public static class Factory {
    private final List<Itinerary> deletedItineraries = new ArrayList<>();

    public ItineraryFilter wrap(ItineraryFilter original) {
      if(!original.removeItineraries()) { return original; }
      else { return new DebugFilterWrapper(original, deletedItineraries); }
    }
  }

  /* private methods */

  private void markItineraryAsDeleted(Itinerary itinerary) {
    itinerary.addSystemNotice(new SystemNotice(
        delegate.name(),
        "This itinerary is marked as deleted by the " + delegate.name() + " filter. "
    ));
  }

}
