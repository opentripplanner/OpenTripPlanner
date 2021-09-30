package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryListFilter;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DebugFilterWrapper implements ItineraryListFilter {

  private final ItineraryListFilter delegate;
  private final List<Itinerary> deletedItineraries;

  public DebugFilterWrapper(ItineraryListFilter delegate, List<Itinerary> deletedItineraries) {
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

  @Override
  public boolean removeItineraries() { return false; }

  private List<Itinerary> remindingItineraries(List<Itinerary> originalList) {
    return originalList.stream()
        .filter(it -> !deletedItineraries.contains(it))
        .collect(Collectors.toList());
  }


  /* inner classes */

  public static class Factory {
    private final List<Itinerary> deletedItineraries = new ArrayList<>();

    public ItineraryListFilter wrap(ItineraryListFilter original) {
      if(!original.removeItineraries()) { return original; }
      else { return new DebugFilterWrapper(original, deletedItineraries); }
    }
  }

  /* private methods */

  private void markItineraryAsDeleted(Itinerary itinerary) {
    deletedItineraries.add(itinerary);
    itinerary.markAsDeleted(new SystemNotice(
        delegate.name(),
        "This itinerary is marked as deleted by the " + delegate.name() + " filter. "
    ));
  }

}
