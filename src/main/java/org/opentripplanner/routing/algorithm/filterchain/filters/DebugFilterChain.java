package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.SystemNotice;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;

import java.util.List;

public class DebugFilterChain implements ItineraryFilter {
    private final List<ItineraryFilter> filters;

    public DebugFilterChain(List<ItineraryFilter> filters) {
        this.filters = filters;
    }

    @Override
    public String name() {
        return "debug-filter-chain";
    }

    @Override
    public List<Itinerary> filter(List<Itinerary> itineraries) {
        List<Itinerary> current = itineraries;
        List<Itinerary> last = itineraries;

        for (ItineraryFilter filter : filters) {
            current = filter.filter(last);

            if(current.size() < last.size()) {
                for (Itinerary it : last) {
                    // match by instance ref, see Itinerary#equals
                    if(!current.contains(it)) {
                        markItineraryAsDeleted(filter.name(), it);
                    }
                }
            }
            else {
                last = current;
            }
        }
        return current;
    }

    private void markItineraryAsDeleted(String filterName, Itinerary itinerary) {
        itinerary.addSystemNotice(new SystemNotice(
                filterName,
                "This itinerary is marked as deleted by the " + filterName + " filter. "
        ));
    }
}
