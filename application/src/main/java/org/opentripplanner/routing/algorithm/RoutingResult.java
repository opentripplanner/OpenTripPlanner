package org.opentripplanner.routing.algorithm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import org.opentripplanner.model.plan.Itinerary;
import org.opentripplanner.routing.api.response.RoutingError;

/**
 * This class maintain routing results/itineraries and errors. It is used to merge set of
 * itineraries from multiple routers (direct street/transit/flex) into one set. It also has
 * methods to transform the current set of itineraries to a new set, see {@link #transform(Function)}.
 */
class RoutingResult {

  private final List<Itinerary> itineraries = new ArrayList<>();
  private final Set<RoutingError> errors = new HashSet<>();

  RoutingResult(Collection<Itinerary> itineraries, Collection<RoutingError> errors) {
    addItineraries(itineraries);
    addErrors(errors);
  }

  static RoutingResult empty() {
    return new RoutingResult(List.of(), List.of());
  }

  static RoutingResult ok(List<Itinerary> itineraries) {
    return new RoutingResult(itineraries, List.of());
  }

  static RoutingResult failed(Collection<RoutingError> errors) {
    return new RoutingResult(List.of(), errors);
  }

  List<Itinerary> itineraries() {
    return List.copyOf(itineraries);
  }

  Set<RoutingError> errors() {
    return Set.copyOf(errors);
  }

  void merge(RoutingResult... others) {
    for (RoutingResult it : others) {
      addItineraries(it.itineraries);
      addErrors(it.errors);
    }
  }

  /**
   * This method is used to decorate the itineraries with additonal information. The itinerary
   * filter chain is the main place to add decorating filters. This is ment to extend/support the
   * router logic - maybe compansate for diffrences in the specific routers. For example setting
   * the {@code generalizedCost2} for direct FLEX results, to allow them to be compared with
   * transit results where the transit router alreaddy calculated the {@code generalizedCost2}
   * value. This should not be used for things like fares witch is just adding more info to the
   * itineraries, this should be done in the filter chain. Another way to look at it is to use
   * this for services witch must allways run, while the filter-chain is for configurable
   * (optional) features.
   */
  void transform(Function<List<Itinerary>, List<Itinerary>> transform) {
    var list = transform.apply(this.itineraries);
    if (this.itineraries != list) {
      this.itineraries.clear();
      this.itineraries.addAll(list);
    }
  }

  void addErrors(Collection<RoutingError> errors) {
    this.errors.addAll(errors);
  }

  /* private methods */

  private void addItineraries(Collection<Itinerary> itineraries) {
    this.itineraries.addAll(itineraries);
  }
}
