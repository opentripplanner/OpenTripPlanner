package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import java.util.Comparator;
import java.util.List;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.model.plan.StreetLeg;
import org.opentripplanner.model.plan.TransitLeg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.GroupId;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * This class creates a group identifier for an itinerary based on the longest legs which together
 * account for more than 'p' part of the total distance. Transit legs must overlap and ride the
 * same trip, while street-legs only need to have the same mode. We call the set of legs the
 * 'key-set-of-legs' or just 'key-set'.
 * <p>
 * Two itineraries can be almost identical, but still have differences in the size of the key-set.
 * Riding any trip just one extra stop might include/exclude a leg in/from the key-set. To account
 * for this, we say two itineraries, A and B, are the same if the key-set of A is contained in B OR
 * the key-set of B is contained in A. Any "extra" legs in the key-set is ignored.
 * <p>
 * Two transit legs are considered the same if they are riding the same transit trip and overlap in
 * time. So, for example where a transfer happens do not affect the result, unless one of the legs
 * fall out of the key-set. They must overlap in time to account for looping patterns - a pattern
 * visiting the same stops more than once.
 * <p>
 * This filter does not support grouping street only-itineraries, but it does support having a
 * street leg as part of the key. At least one transit leg must be part of the key. This is done
 * because we only want to group itineraries that overlap in time. We only use the mode when
 * comparing street legs, not the place or time. By adding one transit leg to the key, we make sure
 * that the journey overlap in place and time.
 * <p>
 * When comparing the street legs part of the key, we only care about the mode. If there are more
 * than one street leg in the key, then other keys must match. The street legs do not need to
 * overlap in time or place. For example, two itineraries, one with a long walking access and
 * the other with a long egress, are considered the same - we ignore the fact that the walking
 * happens in the beginning and the end of the journey.
 */
public class GroupByDistance implements GroupId<GroupByDistance> {

  private final List<Leg> keySet;
  private final boolean streetOnly;

  /**
   * @param p 'p' must be between 0.50 (50%) and 0.99 (99%).
   */
  public GroupByDistance(Itinerary itinerary, double p) {
    assertPIsValid(p);
    double limit = p * calculateTotalDistance(itinerary.legs());
    this.streetOnly = itinerary.isStreetOnly();
    this.keySet = createKeySetOfLegsByLimit(itinerary.legs(), limit);
  }

  @Override
  public boolean match(GroupByDistance other) {
    if (this == other) {
      return true;
    }

    // Do not group street only with transit itineraries
    if (streetOnly != other.streetOnly) {
      return false;
    }

    // If two keys are different in length, then we want to use the shortest key and
    // make sure all elements in it also exist in the other. We ignore the extra legs in
    // the longest key-set.
    return size() > other.size() ? contains(other) : other.contains(this);
  }

  @Override
  public GroupByDistance merge(GroupByDistance other) {
    return size() <= other.size() ? this : other;
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(GroupByDistance.class)
      .addBoolIfTrue("streetOnly", streetOnly)
      .addCol("keySet", keySet, GroupByDistance::keySetToString)
      .toString();
  }

  /** package local to be unit-testable */
  int size() {
    return keySet.size();
  }

  /** package local to be unit-testable */
  static double calculateTotalDistance(List<Leg> legs) {
    return legs.stream().mapToDouble(Leg::getDistanceMeters).sum();
  }

  /** package local to be unit-testable */
  static List<Leg> createKeySetOfLegsByLimit(List<Leg> legs, double distanceLimitMeters) {
    // Sort legs descending on distance
    legs = legs
      .stream()
      .sorted(Comparator.comparingDouble(Leg::getDistanceMeters).reversed())
      .toList();

    double sum = 0.0;
    int i = 0;

    while (sum < distanceLimitMeters) {
      // If the transit legs is not long enough, threat the itinerary as non-transit
      if (i == legs.size()) {
        throw new IllegalStateException("Did not expect to get here...");
      }
      sum += legs.get(i).getDistanceMeters();
      ++i;
    }
    return legs.stream().limit(i).toList();
  }

  /** Read-only, package lacal access for unit-test access. */
  List<Leg> getKeySet() {
    return List.copyOf(keySet);
  }

  /* private methods */

  /**
   * Compare to set of legs and return {@code true} if the two sets contains the same set. If the
   * sets are different in size any extra elements are ignored.
   */
  private boolean contains(GroupByDistance other) {
    for (Leg leg : other.keySet) {
      if (keySet.stream().noneMatch(leg::isPartiallySameLeg)) {
        return false;
      }
    }
    return true;
  }

  private void assertPIsValid(double p) {
    if (p > 0.99 || p < 0.50) {
      throw new IllegalArgumentException("'p' is not between 0.01 and 0.99: " + p);
    }
  }

  private static String keySetToString(Leg leg) {
    var builder = ToStringBuilder.of(leg.getClass())
      .addTime("start", leg.getStartTime())
      .addTime("end", leg.getStartTime());

    if (leg instanceof TransitLeg trLeg) {
      builder.addEnum("mode", trLeg.getMode()).addObj("tripId", leg.getTrip().getId());
    } else if (leg instanceof StreetLeg stLeg) {
      builder.addEnum("mode", stLeg.getMode());
    } else {
      throw new IllegalStateException("Unhandled type: " + leg.getClass());
    }
    return builder.toString();
  }
}
