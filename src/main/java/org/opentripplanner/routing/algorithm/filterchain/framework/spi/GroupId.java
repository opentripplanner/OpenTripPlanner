package org.opentripplanner.routing.algorithm.filterchain.framework.spi;

/**
 * A group-id identify a group of elements(itineraries). Group-ids can be arranged in a hierarchy
 * where the top level groups match all elements for all decedent group-ids of children,
 * grandchildren, and so on.
 * <p>
 * Lets, look at an example to see why we need this. Let say we want to group all itineraries by
 * their longest leg measured in distance. We use the legs that account for at least 68% (standard
 * deviation) of the total transit distance. Then for a trip with legs X-Y-X with distance 20-70-10,
 * the set of legs {Y} is enough to be used as a group-id (70% of the total distance, 100). We want
 * to find all trips containing {Y} and group them together. For another trip X-Y-W the distances
 * are 20-70-19, hence the group-id becomes {Y,W}(83%), not just {Y}(64%). Y is the longest leg so Y
 * is placed first in the <em>ordered</em> set. When we group the trips together we do <em>not</em>
 * want these 2 trips to be in separate groups, so we merge the second group into the first - we can
 * do this since trips with group-id {Y} include all trips that also match {Y,W}. We say that {Y} is
 * of a higher order than {Y, W} - because it have less elements in the set.
 * <pre>
 * Given trip a, b and c; with legs X, Y and/or Z:
 * a := X-Y
 * b := X-Y-Z
 * c := Z
 *
 * Then:
 * // The match method should be symmetric
 * a.match(b) === b.match(a)
 * a.match(c) === c.match(a)
 *
 * // 'a' contains all itineraries that 'b' contains
 * a.match(b) => TRUE
 * b.match(a) => TRUE
 * a.orderHigherOrEq(b) => TRUE
 * b.orderHigherOrEq(a) => FALSE
 *
 * // 'a' and 'c' does not match
 * a.match(c) => FALSE
 * c.match(a) => FALSE
 * a.orderHigherOrEq(c) => FALSE
 * c.orderHigherOrEq(a) => TRUE    // the order of c is higher than a
 *
 * // For to identical group-id's
 * e := X-Y
 * f := X-Y
 *
 * e.match(f) => TRUE
 * e.orderHigherOrEq(f) => TRUE
 * </pre>
 */
public interface GroupId<T extends GroupId<T>> {
  /**
   * @see GroupId for description on the match method.
   */
  boolean match(T other);

  /**
   * There are cases where two groupIds A and B is no symmetrical. Let say A can be used as a
   * group-id for both itinerary I and II, but B is only valid as a groupId for II. Then this method
   * should return A for both: {@code  A.merge(B)} and {@code B.merge(A)}. If both group-ids can be
   * used or are identical, any of them can be returned.
   * <p>
   * The reason why we need this is best explained with an example: We want to group by the legs
   * that pose 80% of the distance traveled. We have the following possible itineraries:
   * <pre>
   *   I : Origin ~ Bus A 100 km ~ Destination
   *  II : Origin ~ Bus B 15 km ~ Bus A 85 km ~ Destination
   * III : Origin ~ Bus B 25 km ~ Bus A 75 km ~ Destination
   *  IV : Origin ~ Bus A 75 km ~ Bus D 25 km ~ Destination
   * </pre>
   * Concatenating the trip-ids(A,B,C,D) ordered by distance gives us the following group-ids:
   * <pre>
   *   I : "A"
   *  II : "A"    // B account for less than 20% of the distance
   * III : "AB"   // A (75%) is longer; hence comes first and than B (25%) > 80%
   *  IV : "AD"   // A is longer, then D
   * </pre>
   * So all of these trips have the same main part "A" and we want to group them together. We do so
   * by comparing the keys, if one is a prefix of another then they belong to the same group. But
   * what about III and IV? By them self they are not the same group, but if we use the groupId for
   * A or B ad a key to the group, both fall into the same group. So, to be deterministic we use
   * this {@code merge(..)} method to return the group-id that is the most general one - the id able
   * to represent the biggest set of trips.
   */
  T merge(T other);
}
