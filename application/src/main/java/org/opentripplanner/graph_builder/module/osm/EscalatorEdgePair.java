package org.opentripplanner.graph_builder.module.osm;

import javax.annotation.Nullable;
import org.opentripplanner.street.model.edge.EscalatorEdge;

public record EscalatorEdgePair(@Nullable EscalatorEdge main, @Nullable EscalatorEdge back) {}
