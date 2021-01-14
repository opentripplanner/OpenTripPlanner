package org.opentripplanner.ext.vectortiles;


import com.wdtinc.mapbox_vector_tile.VectorTile;
import org.geotools.geometry.Envelope2D;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.common.geometry.WebMercatorTile;
import org.opentripplanner.ext.vectortiles.layers.bikerental.BikeRentalLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.stations.StationsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.stops.StopsLayerBuilder;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.config.VectorTileConfig;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.WorldEnvelope;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Path("/routers/{ignoreRouterId}/vectorTiles")
public class VectorTilesResource {
  enum LayerType {Stop, Station, BikeRental}

  static Map<LayerType, BiFunction<Graph, LayerParameters, LayerBuilder>> layers = new HashMap<>();

  static {
    layers.put(LayerType.Stop, StopsLayerBuilder::new);
    layers.put(LayerType.Station, StationsLayerBuilder::new);
    layers.put(LayerType.BikeRental, BikeRentalLayerBuilder::new);
  }

  @Context
  private OTPServer otpServer;

  /**
   * @deprecated The support for multiple routers are removed from OTP2.
   * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
   */
  @Deprecated @PathParam("ignoreRouterId")
  private String ignoreRouterId;

  @GET
  @Path("/{layers}/{z}/{x}/{y}.pbf")
  @Produces("application/x-protobuf")
  public Response tileGet(
      @PathParam("x") int x,
      @PathParam("y") int y,
      @PathParam("z") int z,
      @PathParam("layers") String requestedLayers
  ) throws Exception {
    VectorTile.Tile.Builder mvtBuilder = VectorTile.Tile.newBuilder();

    if (z < VectorTileConfig.MIN_ZOOM) {
      return Response.status(Response.Status.OK).entity(mvtBuilder.build().toByteArray()).build();
    }

    Envelope2D env = WebMercatorTile.tile2Envelope(x, y, z);
    Envelope envelope = new Envelope(env.getMaxX(), env.getMinX(), env.getMaxY(), env.getMinY());

    List<String> layers = Arrays.asList(requestedLayers.split(","));

    Router router = otpServer.getRouter();
    int cacheMaxSeconds = Integer.MAX_VALUE;

    for (LayerParameters layerParameters : router.routerConfig.vectorTileLayers().layers()) {
      if (layers.contains(layerParameters.name())
          && layerParameters.minZoom() <= z
          && z <= layerParameters.maxZoom()
      ) {
        cacheMaxSeconds = Math.min(cacheMaxSeconds, layerParameters.cacheMaxSeconds());
        mvtBuilder.addLayers(VectorTilesResource.layers
            .get(LayerType.valueOf(layerParameters.type()))
            .apply(router.graph, layerParameters)
            .build(envelope));
      }
    }

    CacheControl cacheControl = new CacheControl();
    if (cacheMaxSeconds != Integer.MAX_VALUE) {
      cacheControl.setMaxAge(cacheMaxSeconds);
    }
    byte[] bytes = mvtBuilder.build().toByteArray();
    return Response.status(Response.Status.OK).cacheControl(cacheControl).entity(bytes).build();
  };

  @GET
  @Path("/{layers}/tilejson.json")
  @Produces(MediaType.APPLICATION_JSON)
  public TileJson getTileJson(
      @Context UriInfo uri,
      @Context HttpHeaders headers,
      @PathParam("layers") String requestedLayers
  ) {
    return new TileJson(otpServer.getRouter().graph, uri, headers, requestedLayers);
  }

  private String getBaseAddress(UriInfo uri, HttpHeaders headers, String layers) {
    String protocol;
    if (headers.getRequestHeader("X-Forwarded-Proto") != null) {
      protocol = headers.getRequestHeader("X-Forwarded-Proto").get(0);
    } else {
      protocol = uri.getRequestUri().getScheme();
    }

    String host;
    if (headers.getRequestHeader("X-Forwarded-Host") != null ) {
      host = headers.getRequestHeader("X-Forwarded-Host").get(0);
    } else if (headers.getRequestHeader("Host") != null) {
      host = headers.getRequestHeader("Host").get(0);
    } else {
      host = uri.getBaseUri().getHost() + ":" + uri.getBaseUri().getPort();
    }

    return protocol + "://" +  host;
  }

  private class TileJson implements Serializable {
    public final String tilejson = "2.2.0";
    public final String name = "OpenTripPlanner";
    public final String attribution;
    public final String scheme = "xyz";
    public final String[] tiles;
    public final int minzoom = VectorTileConfig.MIN_ZOOM;
    public final int maxzoom = VectorTileConfig.MAX_ZOOM;
    public final double[] bounds;
    public final double[] center;

    private TileJson(Graph graph, UriInfo uri, HttpHeaders headers, String layers) {
      attribution = graph
          .getFeedIds()
          .stream()
          .map(graph::getFeedInfo)
          .filter(Predicate.not(Objects::isNull))
          .map(feedInfo ->
              "<a href='"
                  + feedInfo.getPublisherUrl()
                  + "'>"
                  + feedInfo.getPublisherName()
                  + "</a>"
          )
          .collect(Collectors.joining(", "));

      tiles = new String[]{
          getBaseAddress(uri, headers, layers) + "/otp/routers/" + ignoreRouterId + "/vectorTiles/{z}/{x}/{y}.pbf"
      };

      WorldEnvelope envelope = graph.getEnvelope();

      bounds = new double[]{
          envelope.getLowerLeftLongitude(),
          envelope.getLowerLeftLatitude(),
          envelope.getUpperRightLongitude(),
          envelope.getUpperRightLatitude()
      };

      center = graph
          .getCenter()
          .map(coordinate -> new double[]{coordinate.x, coordinate.y, 9})
          .orElse(null);
    }
  }

  public interface LayersParameters {
    List<LayerParameters> layers();
  }

  public interface LayerParameters {
    String name();
    String type();
    String mapper();
    int maxZoom();
    int minZoom();
    int cacheMaxSeconds();
  }
}
