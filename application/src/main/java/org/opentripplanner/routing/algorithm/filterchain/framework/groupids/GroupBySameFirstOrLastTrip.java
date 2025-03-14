package org.opentripplanner.routing.algorithm.filterchain.framework.groupids;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.model.plan.Leg;
import org.opentripplanner.routing.algorithm.filterchain.framework.spi.GroupId;
import org.opentripplanner.transit.model.framework.FeedScopedId;

/**
 * This class creates a group identifier for an itinerary based on first or last trip. Two itineraries
 * are considered same if they begin ar end with same trip. Trips are considered equal if they have
 * same id and same service day. Non-transit legs are skipped during comparison.
 */
public class GroupBySameFirstOrLastTrip implements GroupId<GroupBySameFirstOrLastTrip> {

  private final List<Leg> keySet;

  public GroupBySameFirstOrLastTrip(Itinerary itinerary) {
    keySet = itinerary.legs().stream().filter(Leg::isTransitLeg).collect(Collectors.toList());
  }

  @Override
  public boolean match(GroupBySameFirstOrLastTrip other) {
    if (this == other) {
      return true;
    }

    // Itineraries without transit are not filtered - they are considered different
    if (this.keySet.isEmpty() || other.keySet.isEmpty()) {
      return false;
    }

    return isTheSame(this.keySet, other.keySet);
  }

  @Override
  public GroupBySameFirstOrLastTrip merge(GroupBySameFirstOrLastTrip other) {
    return this;
  }

  /**
   * Read-only access to key-set to allow unit-tests access.
   */
  List<Leg> getKeySet() {
    return List.copyOf(keySet);
  }

  private static boolean isTheSame(List<Leg> a, List<Leg> b) {
    var firstLegA = a.get(0);
    var firstLegB = b.get(0);
    var lastLegA = a.get(a.size() - 1);
    var lastLegB = b.get(b.size() - 1);

    return isTheSame(firstLegA, firstLegB) || isTheSame(lastLegA, lastLegB);
  }

  private static boolean isTheSame(Leg a, Leg b) {
    final FeedScopedId idA = a.getTrip() != null ? a.getTrip().getId() : null;
    final FeedScopedId idB = b.getTrip() != null ? b.getTrip().getId() : null;

    if (!Objects.equals(idA, idB)) {
      return false;
    }

    return Objects.equals(a.getServiceDate(), b.getServiceDate());
  }
}
