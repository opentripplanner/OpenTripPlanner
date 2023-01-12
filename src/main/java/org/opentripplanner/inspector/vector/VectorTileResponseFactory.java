package org.opentripplanner.inspector.vector;

import edu.colorado.cires.cmg.mvt.VectorTile;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.resource.WebMercatorTile;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.transit.service.TransitService;

/**
 * Common functionality for creating a vector tile response.
 */
public class VectorTileResponseFactory {

  public static <LayerType extends Enum<LayerType>> Response create(
    int x,
    int y,
    int z,
    Locale locale,
    List<String> requestedLayers,
    List<LayerParameters<LayerType>> availableLayers,
    LayerBuilderFactory<LayerType> layerBuilderFactory,
    Graph graph,
    TransitService transitService
  ) {
    VectorTile.Tile.Builder mvtBuilder = VectorTile.Tile.newBuilder();
    Envelope envelope = WebMercatorTile.tile2Envelope(x, y, z);

    int cacheMaxSeconds = Integer.MAX_VALUE;

    for (LayerParameters<LayerType> layerParameters : availableLayers) {
      if (
        requestedLayers.contains(layerParameters.name()) &&
        layerParameters.minZoom() <= z &&
        z <= layerParameters.maxZoom()
      ) {
        cacheMaxSeconds = Math.min(cacheMaxSeconds, layerParameters.cacheMaxSeconds());
        VectorTile.Tile.Layer layer = layerBuilderFactory
          .crateLayerBuilder(layerParameters, locale, graph, transitService)
          .build(envelope);
        mvtBuilder.addLayers(layer);
      }
    }

    CacheControl cacheControl = new CacheControl();
    if (cacheMaxSeconds != Integer.MAX_VALUE) {
      cacheControl.setMaxAge(cacheMaxSeconds);
    }
    return Response
      .status(Response.Status.OK)
      .cacheControl(cacheControl)
      .entity(mvtBuilder.build().toByteArray())
      .build();
  }

  @FunctionalInterface
  public interface LayerBuilderFactory<LayerType extends Enum<LayerType>> {
    LayerBuilder<?> crateLayerBuilder(
      LayerParameters<LayerType> layerParameters,
      Locale locale,
      Graph graph,
      TransitService transitService
    );
  }
}
