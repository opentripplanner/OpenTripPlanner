package org.opentripplanner.routing.algorithm.filterchain.groupids;


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
     * A higher order groupId refer to a group that potentially would include all elements of a
     * lower order group. Note! They must also {@link #match(GroupId)} for this to be true.
     */
    boolean orderHigherOrEq(T other);
}
