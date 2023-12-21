package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;

public sealed interface TileSource {
  @JsonSerialize
  String type();

  String id();

  /**
   * Represents a vector tile source.
   */
  record VectorSource(String id, String url) implements TileSource {
    @Override
    public String type() {
      return "vector";
    }
  }

  /**
   * Represents a raster-based source for map tiles.
   */
  record RasterSource(String id, List<String> tiles, int tileSize) implements TileSource {
    @Override
    public String type() {
      return "raster";
    }
  }
}
