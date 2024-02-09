package org.opentripplanner.apis.vectortiles.model;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Represents a style specification for Maplibre/Mapbox vector tile layers.
 * https://maplibre.org/maplibre-style-spec/root/
 * <p>
 * Maplibre uses these to render vector maps in the browser.
 */
public final class StyleSpec {

  private final String name;
  private final Collection<TileSource> sources;
  private final List<JsonNode> layers;

  public StyleSpec(String name, Collection<TileSource> sources, List<StyleBuilder> layers) {
    this.name = name;
    this.sources = sources;
    this.layers = layers.stream().map(StyleBuilder::toJson).toList();
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
