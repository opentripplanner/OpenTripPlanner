package org.opentripplanner.standalone.config.routerconfig;

import static org.opentripplanner.ext.vectortiles.VectorTilesResource.LayerParameters.CACHE_MAX_SECONDS;
import static org.opentripplanner.ext.vectortiles.VectorTilesResource.LayerParameters.EXPANSION_FACTOR;
import static org.opentripplanner.ext.vectortiles.VectorTilesResource.LayerParameters.MAX_ZOOM;
import static org.opentripplanner.ext.vectortiles.VectorTilesResource.LayerParameters.MIN_ZOOM;
import static org.opentripplanner.standalone.config.framework.json.OtpVersion.NA;

import java.util.Collection;
import java.util.List;
import org.opentripplanner.ext.vectortiles.VectorTilesResource;
import org.opentripplanner.standalone.config.framework.json.NodeAdapter;

public class VectorTileConfig implements VectorTilesResource.LayersParameters {

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
        .since(NA)
        .summary("TODO")
        .description(/*TODO DOC*/"TODO")
        .asObjects(VectorTileConfig::mapLayer)
    );
  }

  public static Layer mapLayer(NodeAdapter node) {
    return new Layer(
      node.of("name").since(NA).summary("TODO").asString(),
      node.of("type").since(NA).summary("TODO").asEnum(VectorTilesResource.LayerType.class),
      node.of("mapper").since(NA).summary("TODO").asString(),
      node.of("maxZoom").since(NA).summary("TODO").asInt(MAX_ZOOM),
      node.of("minZoom").since(NA).summary("TODO").asInt(MIN_ZOOM),
      node.of("cacheMaxSeconds").since(NA).summary("TODO").asInt(CACHE_MAX_SECONDS),
      node.of("expansionFactor").since(NA).summary("TODO").asDouble(EXPANSION_FACTOR)
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
