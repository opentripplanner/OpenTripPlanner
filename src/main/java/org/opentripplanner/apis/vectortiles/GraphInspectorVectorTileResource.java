package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.framework.io.HttpUtils.APPLICATION_X_PROTOBUF;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import org.glassfish.grizzly.http.server.Request;
import org.opentripplanner.api.model.TileJson;
import org.opentripplanner.apis.vectortiles.model.LayerStyleBuilder;
import org.opentripplanner.apis.vectortiles.model.MapboxStyleJson;
import org.opentripplanner.apis.vectortiles.model.TileSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.RasterSource;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.inspector.vector.VectorTileResponseFactory;
import org.opentripplanner.inspector.vector.geofencing.GeofencingZonesLayerBuilder;
import org.opentripplanner.inspector.vector.stop.AreaStopsLayerBuilder;
import org.opentripplanner.inspector.vector.stop.RegularStopsLayerBuilder;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

/**
 * Slippy map vector tile API for rendering various graph information for inspection/debugging
 * purposes.
 */
@Path("/routers/{ignoreRouterId}/inspector/vectortile")
public class GraphInspectorVectorTileResource {

  private static final List<LayerParameters<LayerType>> DEBUG_LAYERS = List.of(
    new LayerParams("regularStops", LayerType.RegularStop),
    new LayerParams("areaStops", LayerType.AreaStop),
    new LayerParams("geofencingZones", LayerType.GeofencingZones)
  );

  private final OtpServerRequestContext serverContext;
  private final String ignoreRouterId;

  public GraphInspectorVectorTileResource(
    @Context OtpServerRequestContext serverContext,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.serverContext = serverContext;
    this.ignoreRouterId = ignoreRouterId;
  }

  @GET
  @Path("/{layers}/{z}/{x}/{y}.pbf")
  @Produces(APPLICATION_X_PROTOBUF)
  public Response tileGet(
    @Context Request grizzlyRequest,
    @PathParam("x") int x,
    @PathParam("y") int y,
    @PathParam("z") int z,
    @PathParam("layers") String requestedLayers
  ) {
    return VectorTileResponseFactory.create(
      x,
      y,
      z,
      grizzlyRequest.getLocale(),
      Arrays.asList(requestedLayers.split(",")),
      DEBUG_LAYERS,
      GraphInspectorVectorTileResource::createLayerBuilder,
      serverContext
    );
  }

  @GET
  @Path("/{layers}/tilejson.json")
  @Produces(MediaType.APPLICATION_JSON)
  public TileJson getTileJson(
    @Context UriInfo uri,
    @Context HttpHeaders headers,
    @PathParam("layers") String requestedLayers
  ) {
    var envelope = serverContext.worldEnvelopeService().envelope().orElseThrow();
    List<FeedInfo> feedInfos = feedInfos();

    return new TileJson(
      uri,
      headers,
      requestedLayers,
      ignoreRouterId,
      "inspector/vectortile",
      envelope,
      feedInfos
    );
  }

  @GET
  @Path("/style.json")
  @Produces(MediaType.APPLICATION_JSON)
  public MapboxStyleJson getTileJson(@Context UriInfo uri, @Context HttpHeaders headers) {
    var base = HttpUtils.getBaseAddress(uri, headers);
    final String allLayers = DEBUG_LAYERS
      .stream()
      .map(LayerParameters::name)
      .collect(Collectors.joining(","));
    var url =
      base +
      "/otp/routers/" +
      ignoreRouterId +
      "/inspector/vectortile/" +
      allLayers +
      "/tilejson.json";
    var vectorSource = new VectorSource("debug", url);
    var backgroundSource = new RasterSource(
      "background",
      List.of("https://a.tile.openstreetmap.org/{z}/{x}/{y}.png"),
      256
    );
    List<TileSource> sources = List.of(backgroundSource, vectorSource);
    return new MapboxStyleJson(
      "OTP Debug Tiles",
      sources,
      List.of(
        LayerStyleBuilder
          .ofId("background")
          .typeRaster()
          .source(backgroundSource)
          .minZoom(0)
          .maxZoom(22)
          .build(),
        LayerStyleBuilder
          .ofId("regular-stop")
          .source(vectorSource)
          .sourceLayer("regularStops")
          .typeCircle()
          .circleStroke("#140d0e", 1)
          .circleColor("#fcf9fa")
          .minZoom(13)
          .maxZoom(22)
          .build()
      )
    );
  }

  @Nonnull
  private List<FeedInfo> feedInfos() {
    return serverContext
      .transitService()
      .getFeedIds()
      .stream()
      .map(serverContext.transitService()::getFeedInfo)
      .filter(Predicate.not(Objects::isNull))
      .toList();
  }

  private static LayerBuilder<?> createLayerBuilder(
    LayerParameters<LayerType> layerParameters,
    Locale locale,
    OtpServerRequestContext context
  ) {
    return switch (layerParameters.type()) {
      case RegularStop -> new RegularStopsLayerBuilder(
        context.transitService(),
        layerParameters,
        locale
      );
      case AreaStop -> new AreaStopsLayerBuilder(context.transitService(), layerParameters, locale);
      case GeofencingZones -> new GeofencingZonesLayerBuilder(context.graph(), layerParameters);
    };
  }

  private enum LayerType {
    RegularStop,
    AreaStop,
    GeofencingZones,
  }

  private record LayerParams(String name, LayerType type) implements LayerParameters<LayerType> {
    @Override
    public String mapper() {
      return "DebugClient";
    }
  }
}
