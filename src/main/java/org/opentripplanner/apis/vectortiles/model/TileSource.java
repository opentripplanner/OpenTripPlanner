package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.List;

public sealed interface TileSource {
  @JsonSerialize
  String type();

  String id();

  record VectorSource(String id, String url) implements TileSource {
    @Override
    public String type() {
      return "vector";
    }
  }

  record RasterSource(String id, List<String> tiles, int tileSize) implements TileSource {
    @Override
    public String type() {
      return "raster";
    }
  }
}
