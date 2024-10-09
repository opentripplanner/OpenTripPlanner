package org.opentripplanner.apis.vectortiles.model;

import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.inspector.vector.LayerParameters;

public record LayerParams(String name, LayerType type) implements LayerParameters<LayerType> {
  @Override
  public String mapper() {
    return "DebugClient";
  }

  /**
   * Convert these params to a vector source layer so that it can be used in the style for rendering
   * in the frontend.
   */
  public VectorSourceLayer toVectorSourceLayer(VectorSource source) {
    return new VectorSourceLayer(source, name);
  }
}
