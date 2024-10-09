package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.street.model.StreetTraversalPermission;

public record StreetTraversalPermissionPair(
  StreetTraversalPermission main,
  StreetTraversalPermission back
) {}
