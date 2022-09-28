package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.List;
import java.util.stream.Collectors;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VectorTileConfig implements VectorTilesResource.LayersParameters {

  public static final int MIN_ZOOM = 9;
  public static final int MAX_ZOOM = 20;
  public static final int CACHE_MAX_SECONDS = -1;
  public static final double EXPANSION_FACTOR = 0.25d;

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
    private final double expansionFactor;

    public Layer(NodeAdapter node) {
      name =
        node.of("name").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString();
      type =
        node.of("type").withDoc(NA, /*TODO DOC*/"TODO").withExample(/*TODO DOC*/"TODO").asString();
      mapper =
        node
          .of("mapper")
          .withDoc(NA, /*TODO DOC*/"TODO")
          .withExample(/*TODO DOC*/"TODO")
          .asString();
      maxZoom = node.of("maxZoom").withDoc(NA, /*TODO DOC*/"TODO").asInt(MAX_ZOOM);
      minZoom = node.of("minZoom").withDoc(NA, /*TODO DOC*/"TODO").asInt(MIN_ZOOM);
      cacheMaxSeconds =
        node.of("cacheMaxSeconds").withDoc(NA, /*TODO DOC*/"TODO").asInt(CACHE_MAX_SECONDS);
      expansionFactor =
        node.of("expansionFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(EXPANSION_FACTOR);
    }

    @Override
    public String name() {
      return name;
    }

    @Override
    public String type() {
      return type;
    }

    @Override
    public String mapper() {
      return mapper;
    }

    @Override
    public int maxZoom() {
      return maxZoom;
    }

    @Override
    public int minZoom() {
      return minZoom;
    }

    @Override
    public int cacheMaxSeconds() {
      return cacheMaxSeconds;
    }

    @Override
    public double expansionFactor() {
      return expansionFactor;
    }
  }
}
