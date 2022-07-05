package org.opentripplanner.ext.vectortiles;

import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitModel;

@FunctionalInterface
public interface LayerBuilderFactory {
  LayerBuilder create(
    Graph graph,
    TransitModel transitModel,
    VectorTilesResource.LayerParameters layerParameters
  );
}
