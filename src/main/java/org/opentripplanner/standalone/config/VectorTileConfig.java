package org.opentripplanner.standalone.config;

import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VectorTileConfig implements VectorTilesResource.LayersParameters {

  public static final int MIN_ZOOM = 9;
  public static final int MAX_ZOOM = 20;
  public static final int CACHE_MAX_SECONDS = -1;
  public static final double EXPANSION_FACTOR = 0.25d;

  List<VectorTilesResource.LayerParameters> layers;

  public VectorTileConfig(Collection<? extends VectorTilesResource.LayerParameters> layers) {
    this.layers = List.copyOf(layers);
  }

  @Override
  public List<VectorTilesResource.LayerParameters> layers() {
    return layers;
  }

  public static VectorTileConfig mapVectorTilesParameters(
    NodeAdapter root,
    String vectorTileLayers
  ) {
    return new VectorTileConfig(
      root
        .of(vectorTileLayers)
        .withDoc(NA, /*TODO DOC*/"TODO")
        .withExample(/*TODO DOC*/"TODO")
        .withDescription(/*TODO DOC*/"TODO")
        .asObjects(VectorTileConfig::mapLayer)
    );
  }

  public static Layer mapLayer(NodeAdapter node) {
    return new Layer(
      node.of("name").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      node.of("type").withDoc(NA, /*TODO DOC*/"TODO").asEnum(VectorTilesResource.LayerType.class),
      node.of("mapper").withDoc(NA, /*TODO DOC*/"TODO").asString(),
      node.of("maxZoom").withDoc(NA, /*TODO DOC*/"TODO").asInt(MAX_ZOOM),
      node.of("minZoom").withDoc(NA, /*TODO DOC*/"TODO").asInt(MIN_ZOOM),
      node.of("cacheMaxSeconds").withDoc(NA, /*TODO DOC*/"TODO").asInt(CACHE_MAX_SECONDS),
      node.of("expansionFactor").withDoc(NA, /*TODO DOC*/"TODO").asDouble(EXPANSION_FACTOR)
    );
  }

  record Layer(
    String name,
    VectorTilesResource.LayerType type,
    String mapper,
    int maxZoom,
    int minZoom,
    int cacheMaxSeconds,
    double expansionFactor
  )
    implements VectorTilesResource.LayerParameters {}
}
