package org.opentripplanner.street.search.strategy;

import java.io.Serializable;
import java.util.Objects;
import org.opentripplanner.astar.spi.DominanceFunction;
import org.opentripplanner.street.model.edge.StreetEdge;
import org.opentripplanner.street.search.state.State;

/**
 * A class that determines when one search branch prunes another at the same Vertex, and ultimately
 * which solutions are retained. In the general case, one branch does not necessarily win out over
 * the other, i.e. multiple states can coexist at a single Vertex.
 * <p>
 * Even functions where one state always wins (least weight, fastest travel time) are applied within
 * a multi-state shortest path tree because bike rental, car or bike parking, and turn restrictions
 * all require multiple incomparable states at the same vertex. These need the graph to be
 * "replicated" into separate layers, which is achieved by applying the main dominance logic (lowest
 * weight, lowest cost, Pareto) conditionally, only when the two states have identical bike/car/turn
 * direction status.
 * <p>
 * Dominance functions are serializable so that routing requests may passed between machines in
 * different JVMs, for instance in OTPA Cluster.
 */
public abstract class DominanceFunctions implements Serializable, DominanceFunction<State> {

  /**
   * For bike rental, parking, and approaching turn-restricted intersections states are
   * incomparable: they exist on separate planes. The core state dominance logic is wrapped in this
   * public function and only applied when the two states have all these variables in common (are on
   * the same plane).
   */
  @Override
  public boolean betterOrEqualAndComparable(State a, State b) {
    // Does one state represent riding a rented bike and the other represent walking before/after rental?
    if (!a.isCompatibleVehicleRentalState(b)) {
      return false;
    }

    // In case of bike renting, different networks (ie incompatible bikes) are not comparable
    // TODO: Check for vehicle type
    if (a.isRentingVehicle()) {
      if (!Objects.equals(a.getVehicleRentalNetwork(), b.getVehicleRentalNetwork())) {
        return false;
      }
    }

    // Does one state represent driving a vehicle and the other represent walking after the vehicle was parked?
    if (a.isVehicleParked() != b.isVehicleParked()) {
      return false;
    }

    if (a.getCarPickupState() != b.getCarPickupState()) {
      return false;
    }

    // Since a Vertex may be arrived at using a no-thru restricted path and one without such
    // restrictions, treat the two as separate so one doesn't dominate the other.
    if (a.hasEnteredNoThruTrafficArea() != b.hasEnteredNoThruTrafficArea()) {
      return false;
    }

    // we cannot compare the states where one is inside a "no-drop off" zone and one isn't
    if (a.isInsideNoRentalDropOffArea() != b.isInsideNoRentalDropOffArea()) {
      return false;
    }

    /*
     * The OTP algorithm tries hard to never visit the same node twice. This is generally a good idea because it avoids
     * useless loops in the traversal leading to way faster processing time.
     *
     * However there is are certain rare pathological cases where through a series of turn restrictions and/or roadworks
     * you absolutely must visit a vertex twice if you want to produce a result. One example would be a route like this:
     *   https://tinyurl.com/ycqux93g (Note: At the time of writing this Hindenburgstr. is closed due to roadworks.)
     *
     * Therefore, if we are close to the start or the end of a route we allow this.
     *
     * More discussion: https://github.com/opentripplanner/OpenTripPlanner/issues/3393
     *
     * == Bicycles ==
     *
     * We used to allow also loops for bicycles as turn restrictions also apply to them, however
     * this causes problems when the start/destination is close to an area that has a very complex
     * network of edges due to the visibility calculation. In such a case it can lead to timeouts as
     * too many loops are produced.
     *
     * Example: https://github.com/opentripplanner/OpenTripPlanner/issues/3564
     *
     * In any case, cyclists can always get off the bike and push it across the street so not
     * including the loops should still result in a route. Often this will be preferable to
     * taking a detour due to turn restrictions anyway.
     */
    if (
      a.backEdge != b.getBackEdge() &&
      (a.backEdge instanceof StreetEdge) &&
      a.getBackMode() != null &&
      a.getBackMode().isInCar() &&
      a.getRequest().isCloseToStartOrEnd(a.getVertex())
    ) {
      return false;
    }

    // These two states are comparable (they are on the same "plane" or "copy" of the graph).
    return betterOrEqual(a, b);
  }

  /**
   * Return true if the first state "defeats" the second state or at least ties with it in terms of
   * suitability. In the case that they are tied, we still want to return true so that an existing
   * state will kick out a new one. Provide this custom logic in subclasses. You would think this
   * could be static, but in Java for some reason calling a static function will call the one on the
   * declared type, not the runtime instance type.
   */
  protected abstract boolean betterOrEqual(State a, State b);

  public static class MinimumWeight extends DominanceFunctions {

    /** Return true if the first state has lower weight than the second state. */
    @Override
    public boolean betterOrEqual(State a, State b) {
      return a.weight <= b.weight;
    }
  }

  /**
   * This approach is more coherent in Analyst when we are extracting travel times from the optimal
   * paths. It also leads to less branching and faster response times when building large shortest
   * path trees.
   */
  public static class EarliestArrival extends DominanceFunctions {

    /** Return true if the first state has lower elapsed time than the second state. */
    @Override
    public boolean betterOrEqual(State a, State b) {
      return a.getElapsedTimeSeconds() <= b.getElapsedTimeSeconds();
    }
  }

  /**
   * A dominance function that prefers the least walking. This should only be used with walk-only
   * searches because it does not include any functions of time, and once transit is boarded walk
   * distance is constant.
   * <p>
   * It is used when building stop tree caches for egress from transit stops.
   */
  public static class LeastWalk extends DominanceFunctions {

    @Override
    protected boolean betterOrEqual(State a, State b) {
      return a.getWalkDistance() <= b.getWalkDistance();
    }
  }

  /**
   * In this implementation the relation is not symmetric. There are sets of mutually co-dominant
   * states.
   */
  public static class Pareto extends DominanceFunctions {

    @Override
    public boolean betterOrEqual(State a, State b) {
      // The key problem in pareto-dominance in OTP is that the elements of the state vector are not orthogonal.
      // When walk distance increases, weight increases. When time increases weight increases.
      // It's easy to get big groups of very similar states that don't represent significantly different outcomes.
      // Our solution to this is to give existing states some slack to dominate new states more easily.

      final double EPSILON = 1e-4;
      return (
        a.getElapsedTimeSeconds() <= (b.getElapsedTimeSeconds() + EPSILON) &&
        a.getWeight() <= (b.getWeight() + EPSILON)
      );
    }
  }
}
