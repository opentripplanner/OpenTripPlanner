package org.opentripplanner.inspector.vector;

import edu.colorado.cires.cmg.mvt.VectorTile;
import jakarta.ws.rs.core.CacheControl;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.resource.WebMercatorTile;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

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
    OtpServerRequestContext context
  ) {
    VectorTile.Tile.Builder mvtBuilder = VectorTile.Tile.newBuilder();
    Envelope envelope = WebMercatorTile.tile2Envelope(x, y, z);

    int cacheMaxSeconds = Integer.MAX_VALUE;

    var availableLayerNames = availableLayers
      .stream()
      .map(LayerParameters::name)
      .collect(Collectors.toSet());
    if (!availableLayerNames.containsAll(requestedLayers)) {
      return Response.status(Response.Status.NOT_FOUND)
        .header(HttpHeaders.CONTENT_TYPE, HttpUtils.TEXT_PLAIN)
        .entity(
          "Could not find vector tile layer(s). Requested layers: %s. Available layers: %s.".formatted(
              requestedLayers,
              availableLayerNames
            )
        )
        .build();
    }

    for (LayerParameters<LayerType> layerParameters : availableLayers) {
      if (
        requestedLayers.contains(layerParameters.name()) &&
        layerParameters.minZoom() <= z &&
        z <= layerParameters.maxZoom()
      ) {
        cacheMaxSeconds = Math.min(cacheMaxSeconds, layerParameters.cacheMaxSeconds());
        VectorTile.Tile.Layer layer = layerBuilderFactory
          .createLayerBuilder(layerParameters, locale, context)
          .build(envelope);
        mvtBuilder.addLayers(layer);
      }
    }

    CacheControl cacheControl = new CacheControl();
    if (cacheMaxSeconds != Integer.MAX_VALUE) {
      cacheControl.setMaxAge(cacheMaxSeconds);
    }
    return Response.status(Response.Status.OK)
      .cacheControl(cacheControl)
      .entity(mvtBuilder.build().toByteArray())
      .build();
  }

  @FunctionalInterface
  public interface LayerBuilderFactory<LayerType extends Enum<LayerType>> {
    LayerBuilder<?> createLayerBuilder(
      LayerParameters<LayerType> layerParameters,
      Locale locale,
      OtpServerRequestContext context
    );
  }
}
