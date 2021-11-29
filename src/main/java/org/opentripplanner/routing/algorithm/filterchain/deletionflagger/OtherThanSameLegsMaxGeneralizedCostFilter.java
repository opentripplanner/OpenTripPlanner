package org.opentripplanner.routing.algorithm.filterchain.deletionflagger;

import java.util.List;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.opentripplanner.model.Trip;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.filter.GroupByFilter;

/**
 * This filter marks itineraries, which use same trips for most of their legs, but where some
 * itineraries have a much higher cost for the other legs, for deletion. This is similar to {@link
 * TransitGeneralizedCostFilter}, but is used together with {@link GroupByFilter}
 * to filter within the groups.
 */
public class OtherThanSameLegsMaxGeneralizedCostFilter implements ItineraryDeletionFlagger {

    /**
     * How much higher cost do we allow for the non-shared legs before we filter out the itinerary.
     */
    private final double maxCostOtherLegsFactor;

    public OtherThanSameLegsMaxGeneralizedCostFilter(double maxCostOtherLegsFactor) {
        this.maxCostOtherLegsFactor = maxCostOtherLegsFactor;
    }

    @Override
    public String name() {
        return "other-than-same-legs-max-generalized-cost-filter";
    }


    @Override
    public List<Itinerary> getFlaggedItineraries(List<Itinerary> itineraries) {
        if (itineraries.size() < 2) {
            return List.of();
        }

        // Get all transit trips for an itinerary
        Function<Itinerary, Set<Trip>> getTripsForItinerary =
            itinerary -> itinerary.legs.stream()
                .filter(Leg::isTransitLeg)
                .map(Leg::getTrip)
                .collect(Collectors.toSet());

        // Find the trips that are shared between all itineraries
        Set<Trip> commonTrips = itineraries.stream()
            .map(getTripsForItinerary)
            .reduce((a, b) -> {
                a.retainAll(b);
                return a;
            })
            .get();

        // Find the lowest cost of the common legs
        OptionalInt commonCost = itineraries.stream().mapToInt(
            itinerary -> itinerary.legs.stream()
                .filter(Leg::isTransitLeg)
                .filter(leg -> commonTrips.contains(leg.getTrip()))
                .mapToInt(leg -> leg.generalizedCost)
                .sum()
        ).min();

        if (commonCost.isEmpty()) {
            return List.of();
        }

        // Find the lowest cost for any itinerary
        OptionalInt minimumCost = itineraries.stream()
            .mapToInt(itinerary -> itinerary.generalizedCost)
            .min();

        if (minimumCost.isEmpty()) {
            return List.of();
        }

        // Calculate the maximum limit allowed for itinerary cost
        double maxLimit =
                ((minimumCost.getAsInt() - commonCost.getAsInt()) * maxCostOtherLegsFactor)
                        + commonCost.getAsInt();

        return itineraries.stream()
            .filter(it -> it.generalizedCost > maxLimit)
            .collect(Collectors.toList());
    }
}
