package org.opentripplanner.apis.vectortiles.model;

import org.opentripplanner.apis.vectortiles.DebugStyleSpec;
import org.opentripplanner.inspector.vector.LayerParameters;

public record LayerParams(String name, LayerType type) implements LayerParameters<LayerType> {
  @Override
  public String mapper() {
    return "DebugClient";
  }

  public DebugStyleSpec.VectorSourceLayer toVectorSourceLayer(TileSource.VectorSource source) {
    return new DebugStyleSpec.VectorSourceLayer(source, name);
  }
}
