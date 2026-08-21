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
  private final List<String> rentalNetworks;

  public StyleSpec(
    String name,
    Collection<TileSource> sources,
    List<StyleBuilder> layers,
    List<String> rentalNetworks
  ) {
    this.name = name;
    this.sources = sources;
    this.layers = layers.stream().map(StyleBuilder::toJson).toList();
    this.rentalNetworks = List.copyOf(rentalNetworks);
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

  /**
   * Style-level metadata, which the spec allows to carry arbitrary keys. The debug client uses
   * {@code rentalNetworks} to offer a per-network filter on the vehicle rental layers; the values
   * cannot be derived from the tiles, since a tile only tells you about the networks present in
   * the area currently loaded.
   */
  @JsonSerialize
  public Map<String, Object> metadata() {
    return Map.of("rentalNetworks", rentalNetworks);
  }

  @JsonSerialize
  public String glyphs() {
    return "https://cdn.jsdelivr.net/gh/klokantech/klokantech-gl-fonts@master/{fontstack}/{range}.pbf";
  }
}
