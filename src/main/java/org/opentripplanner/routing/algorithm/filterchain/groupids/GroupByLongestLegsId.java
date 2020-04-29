package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This class create a group identifier for an itinerary based on the set of the longest legs witch
 * together account for more than 'p' part of the total distance. The exact value of {@code p} is be
 * passed in to the constructor.
 * <p>
 * The legs are sorted on distance in decreasing order and added to the group id until they account
 * for more than {@code p} of the total distance.
 */
public class GroupByLongestLegsId implements GroupId<GroupByLongestLegsId> {
    private final List<Leg> longLegs;

    /**
     * @param p 'p' must be between 0.1 (10%) and 0.9 (90%).
     */
    public GroupByLongestLegsId(Itinerary itinerary, double p) {
        assertPIsValid(p);
        List<Leg> transitLegs = new ArrayList<>(itinerary.legs);
        double limit = p * calculateTotalDistance(transitLegs);
        longLegs = findLegsByLimit(transitLegs, limit);
    }

    @Override
    public boolean orderHigherOrEq(GroupByLongestLegsId other) {
        return longLegs.size() <= other.longLegs.size();
    }

    @Override
    public boolean match(GroupByLongestLegsId other) {
        if (this == other) { return true; }

        int len = Math.min(longLegs.size(), other.longLegs.size());

        for (int i = 0; i < len; i++) {
            Leg a = longLegs.get(i);
            Leg b = other.longLegs.get(i);
            if(!a.sameStartAndEnd(b)) { return false; }
        }
        return true;
    }

    void assertPIsValid(double p) {
        if(p > 0.9 || p < 0.1) {
            throw new IllegalArgumentException("'p' is not between 0.1 and 0.9: " +  p);
        }
    }

    static List<Leg> filterTransitLegs(List<Leg> legs) {
        return legs.stream()
                .filter(Leg::isTransitLeg)
                .collect(Collectors.toList());
    }

    static double calculateTotalDistance(List<Leg> transitLegs) {
        return transitLegs.stream().mapToDouble(it -> it.distanceMeters).sum();
    }

    static List<Leg> findLegsByLimit(List<Leg> legs, double limit) {
        // Sort legs descending on distance
        legs = legs.stream()
                .sorted((l,r) -> r.distanceMeters.compareTo(l.distanceMeters))
                .collect(Collectors.toList());
        int i=0;
        double sum = 0.0;
        while ( sum < limit) {
            sum += legs.get(i).distanceMeters;
            ++i;
        }
        return legs.stream()
                .limit(i)
                .collect(Collectors.toList());
    }
}
