package org.opentripplanner.gtfs.mapping;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import java.util.Collection;
import org.onebusaway.gtfs.model.RouteNetworkAssignment;
import org.opentripplanner.core.model.id.FeedScopedId;

/**
 * Keeps the mapping between GTFS route and their network IDs. These assignments don't result
 * in a separate entity in the OTP model, but are used to assign networks to routes.
 */
class RouteNetworkAssignmentMapper {

  private final Multimap<FeedScopedId, FeedScopedId> assignments = ArrayListMultimap.create();

  private final IdFactory idFactory;

  RouteNetworkAssignmentMapper(IdFactory idFactory) {
    this.idFactory = idFactory;
  }

  void map(Collection<RouteNetworkAssignment> assignments) {
    assignments.forEach(a -> {
      var routeId = idFactory.createId(a.getRoute().getId(), "route network assignment's route");
      var networkId = idFactory.createId(a.getNetworkId(), "route network assignment's network");
      this.assignments.put(routeId, networkId);
    });
  }

  /**
   * For the route id given in {@code route}, find all networks that are assigned to it.
   */
  Collection<FeedScopedId> findNetworks(FeedScopedId route) {
    return assignments.get(route);
  }
}
