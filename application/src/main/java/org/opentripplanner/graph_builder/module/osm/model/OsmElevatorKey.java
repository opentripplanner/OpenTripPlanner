package org.opentripplanner.graph_builder.module.osm.model;

import org.opentripplanner.street.model.vertex.OsmEntityType;

public record OsmElevatorKey(long nodeId, OsmEntityType osmEntityType, long entityId) {}
