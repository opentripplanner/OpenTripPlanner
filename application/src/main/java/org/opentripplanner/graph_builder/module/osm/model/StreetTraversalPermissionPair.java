package org.opentripplanner.graph_builder.module.osm.model;

import org.opentripplanner.street.model.StreetTraversalPermission;

public record StreetTraversalPermissionPair(
  StreetTraversalPermission main,
  StreetTraversalPermission back
) {}
