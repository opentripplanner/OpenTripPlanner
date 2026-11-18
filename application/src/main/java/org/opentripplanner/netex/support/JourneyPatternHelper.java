package org.opentripplanner.netex.support;

import org.entur.netex2.model.JourneyPattern_VersionStructure;
import org.entur.netex2.model.Route;
import org.opentripplanner.netex.index.api.ReadOnlyHierarchicalMap;

/**
 * Utility class with helpers for the generated NeTEx XML model classes
 */
public class JourneyPatternHelper {

  private JourneyPatternHelper() {}

  /*
   * Get lineId from RouteRef (Nordic profile) or RouteView (EPIP)
   *
   * @param routeById needed in Nordic profile to look up the route reference
   * @param journeyPattern the JourneyPatten to get the lineId from
   */
  public static String getLineFromRoute(
    ReadOnlyHierarchicalMap<String, Route> routeById,
    JourneyPattern_VersionStructure journeyPattern
  ) {
    String lineRef = null;
    if (journeyPattern.getRouteRef() != null) {
      String routeRef = journeyPattern.getRouteRef().getRef();
      lineRef = routeById.lookup(routeRef).getLineRef().getValue().getRef();
    } else if (journeyPattern.getRouteView() != null) {
      lineRef = journeyPattern.getRouteView().getLineRef().getValue().getRef();
    }
    return lineRef;
  }
}
