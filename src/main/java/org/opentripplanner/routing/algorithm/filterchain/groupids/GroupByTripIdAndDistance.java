package org.opentripplanner.routing.algorithm.filterchain.groupids;

import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;

import java.util.List;
import java.util.stream.Collectors;

/**
 * This class create a group identifier for an itinerary based on the set of the longest
 * transit legs which together account for more than 'p' part of the total distance(including
 * non-transit). We call this set of legs the 'key-set'. Non-transit itineraries all get their own
 * group; Hence are considered different. Only transit legs can be part of the key.
 * <p>
 * Two itineraries can be almost identical, but still have differences in the size of the
 * key-set. Riding any trip just one extra stop might include/exclude a leg in/from the key-set.
 * To account for this, we say two itineraries, A and B, are the same if the key-set of A is
 * contained in B OR the key-set of B is contained in A. Any "extra" legs in the key-set is ignored.
 * <p>
 * Two legs are considered the same if they are riding the same transit trip and overlap in time.
 * So, for example where a transfer happens do not affect the result, unless one of the legs fall
 * out of the key-set. They must overlap in time to account for looping patterns - a pattern
 * visiting the same stops more than once.
 */
public class GroupByTripIdAndDistance implements GroupId<GroupByTripIdAndDistance> {
    private final List<Leg> keySet;

    /**
     * @param p 'p' must be between 0.50 (50%) and 0.99 (99%).
     */
    public GroupByTripIdAndDistance(Itinerary itinerary, double p) {
        assertPIsValid(p);
        List<Leg> transitLegs = itinerary.legs
            .stream()
            .filter(Leg::isTransitLeg)
            .collect(Collectors.toList());

        if(transitLegs.isEmpty()) {
            keySet = List.of();
        }
        else {
            double limit = p * calculateTotalDistance(itinerary.legs);
            keySet = getKeySetOfLegsByLimit(transitLegs, limit);
        }
    }

    @Override
    public GroupByTripIdAndDistance merge(GroupByTripIdAndDistance other) {
        return keySet.size() <= other.keySet.size() ? this : other;
    }

    @Override
    public boolean match(GroupByTripIdAndDistance other) {
        if (this == other) { return true; }

        // Itineraries without transit is not filtered - they are considered different
        if(this.keySet.isEmpty() || other.keySet.isEmpty()) { return false; }

        return isTheSame(this.keySet, other.keySet);
    }

    /** package local to be unit-testable */
    static double calculateTotalDistance(List<Leg> transitLegs) {
        return transitLegs.stream().mapToDouble(it -> it.getDistanceMeters()).sum();
    }

    /** package local to be unit-testable */
    static List<Leg> getKeySetOfLegsByLimit(List<Leg> legs, double distanceLimitMeters) {
        // Sort legs descending on distance
        legs = legs.stream()
                .sorted((l,r) -> r.getDistanceMeters().compareTo(l.getDistanceMeters()))
                .collect(Collectors.toList());
        double sum = 0.0;
        int i=0;
        while (sum < distanceLimitMeters) {
            // If the transit legs is not long enough, threat the itinerary as non-transit
            if(i == legs.size()) { return List.of(); }
            sum += legs.get(i).getDistanceMeters();
            ++i;
        }
        return legs.stream()
                .limit(i)
                .collect(Collectors.toList());
    }

    /** Read-only access to key-set to allow unit-tests access. */
    List<Leg> getKeySet() {
        return List.copyOf(keySet);
    }

    @Override
    public String toString() { return keySet.toString(); }


    /* private methods */

    private void assertPIsValid(double p) {
        if(p > 0.99 || p < 0.50) {
            throw new IllegalArgumentException("'p' is not between 0.01 and 0.99: " +  p);
        }
    }

    /**
     * Compare to set of legs and return {@code true} if the two sets contains the same
     * set. If the sets are different in size any extra elements are ignored.
     */
    private static boolean isTheSame(List<Leg> a, List<Leg> b) {
        // If a and b is different in length, than we want to use the shortest list and
        // make sure all elements in it also exist in the other. We ignore the extra legs in
        // the longer list og legs. We do this by making sure 'a' is the shortest list, if not
        // we swap a and b.
        if(a.size() > b.size()) {
            List<Leg> temp = a;
            a = b;
            b = temp;
        }

        for (final Leg aLeg : a) {
            if (b.stream().noneMatch(aLeg::isPartiallySameTransitLeg)) {
                return false;
            }
        }
        return true;
    }
}
