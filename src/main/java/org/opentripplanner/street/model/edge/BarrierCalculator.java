package org.opentripplanner.street.model.edge;

import org.opentripplanner.street.model.StreetTraversalPermission;
import org.opentripplanner.street.model.vertex.BarrierVertex;
import org.opentripplanner.street.model.vertex.Vertex;

class BarrierCalculator {

  static StreetTraversalPermission reducePermissions(
    StreetTraversalPermission permission,
    Vertex fromv,
    Vertex tov
  ) {
    if (fromv instanceof BarrierVertex bv) {
      permission = permission.intersection(bv.getBarrierPermissions());
    }
    if (tov instanceof BarrierVertex bv) {
      permission = permission.intersection(bv.getBarrierPermissions());
    }
    return permission;
  }
}
