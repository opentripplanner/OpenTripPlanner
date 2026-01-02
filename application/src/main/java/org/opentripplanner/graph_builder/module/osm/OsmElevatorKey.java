package org.opentripplanner.graph_builder.module.osm;

import org.opentripplanner.street.model.vertex.OsmEntityType;

record OsmElevatorKey(long nodeId, OsmEntityType osmEntityType, long entityId) {}
