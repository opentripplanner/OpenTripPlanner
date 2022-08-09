package org.opentripplanner.ext.vectortiles;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitService;

@FunctionalInterface
public interface LayerBuilderFactory {
  LayerBuilder<?> create(
    Graph graph,
    TransitService transitService,
    VectorTilesResource.LayerParameters layerParameters
  );
}
