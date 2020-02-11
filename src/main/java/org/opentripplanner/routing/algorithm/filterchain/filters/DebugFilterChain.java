package org.opentripplanner.routing.algorithm.filterchain.filters;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.alertpatch.Alert;
import org.opentripplanner.routing.alertpatch.AlertPatch;
import org.opentripplanner.routing.algorithm.filterchain.ItineraryFilter;
import org.opentripplanner.util.NonLocalizedString;

import java.util.List;
import java.util.Optional;

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
        itinerary.markAsDeleted();

        // Add a alert to the first transit leg.
        Optional<Leg> legOp = itinerary.firstTransitLeg();
        if(legOp.isEmpty()) { return; }

        Alert alert = new Alert();
        alert.alertHeaderText = new NonLocalizedString(filterName);
        alert.alertDetailText = new NonLocalizedString(
                "This itinerary is marked as deleted by " + filterName + " filter. "
        );
        alert.alertDescriptionText = alert.alertDetailText;

        AlertPatch patch = new AlertPatch();
        patch.setAlert(alert);
        patch.setId(filterName);

        legOp.get().addAlert(alert);
        legOp.get().addAlertPatch(patch);
    }
}
