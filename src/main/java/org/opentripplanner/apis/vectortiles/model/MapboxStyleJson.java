package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class MapboxStyleJson {

  private final String name;
  private final List<TileSource> sources;
  private final List<JsonNode> layers;

  public MapboxStyleJson(String name, List<TileSource> sources, List<JsonNode> layers) {
    this.name = name;
    this.sources = sources;
    this.layers = layers;
  }

  @JsonSerialize
  public int version() {
    return 8;
  }

  @JsonSerialize
  public String name() {
    return name;
  }

  @JsonSerialize
  public Map<String, TileSource> sources() {
    var output = new HashMap<String, TileSource>();
    sources.forEach(s -> output.put(s.id(), s));
    return output;
  }

  @JsonSerialize
  public List<JsonNode> layers() {
    return layers;
  }
}
