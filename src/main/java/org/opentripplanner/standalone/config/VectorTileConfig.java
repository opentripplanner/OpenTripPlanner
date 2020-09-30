package org.opentripplanner.standalone.config;

import org.opentripplanner.ext.vectortiles.VectorTilesResource;

import java.util.List;
import java.util.stream.Collectors;

public class VectorTileConfig implements VectorTilesResource.LayersParameters {
  public static final int MIN_ZOOM = 9;
  public static final int MAX_ZOOM = 20;
  public static final int CACHE_MAX_SECONDS = -1;

  List<VectorTilesResource.LayerParameters> layers;

  public VectorTileConfig(List<NodeAdapter> vectorTileLayers) {
    layers = vectorTileLayers.stream().map(Layer::new).collect(Collectors.toList());
  }

  @Override
  public List<VectorTilesResource.LayerParameters> layers() {
    return layers;
  }

  static class Layer implements VectorTilesResource.LayerParameters {

    private final String name;
    private final String type;
    private final String mapper;
    private final Integer maxZoom;
    private final Integer minZoom;
    private final Integer cacheMaxSeconds;

    public Layer(NodeAdapter node) {
      name = node.asText("name");
      type = node.asText("type");
      mapper = node.asText("mapper");
      maxZoom = node.asInt("maxZoom", MAX_ZOOM);
      minZoom = node.asInt("minZoom", MIN_ZOOM);
      cacheMaxSeconds = node.asInt("cacheMaxSeconds", CACHE_MAX_SECONDS);
    }

    @Override public String name() { return name; }
    @Override public String type() { return type; }
    @Override public String mapper() { return mapper; }
    @Override public int maxZoom() { return maxZoom; }
    @Override public int minZoom() { return minZoom; }
    @Override public int cacheMaxSeconds() {return cacheMaxSeconds; }
  }
}
