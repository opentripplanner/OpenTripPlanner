package org.opentripplanner.graph_builder.module.osm.naming;

import java.util.Set;
import org.opentripplanner.osm.model.OsmLevel;
import org.opentripplanner.osm.model.OsmWay;
import org.opentripplanner.street.model.edge.StreetEdge;

public record EdgeOnLevel(OsmWay way, StreetEdge edge, Set<OsmLevel> levels) {}
