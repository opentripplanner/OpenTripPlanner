package org.opentripplanner.osm.wayproperty;

import org.opentripplanner.street.model.StreetTraversalPermission;

public record MixinDirectionalProperties(
  StreetTraversalPermission addedPermission,
  StreetTraversalPermission removedPermission,
  double walkSafety,
  double bicycleSafety
) {}
