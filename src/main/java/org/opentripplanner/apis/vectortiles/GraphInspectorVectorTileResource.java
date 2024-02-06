package org.opentripplanner.apis.vectortiles;

import static org.opentripplanner.apis.vectortiles.model.LayerType.AreaStop;
import static org.opentripplanner.apis.vectortiles.model.LayerType.Edge;
import static org.opentripplanner.apis.vectortiles.model.LayerType.GeofencingZones;
import static org.opentripplanner.apis.vectortiles.model.LayerType.GroupStop;
import static org.opentripplanner.apis.vectortiles.model.LayerType.RegularStop;
import static org.opentripplanner.apis.vectortiles.model.LayerType.Vertex;
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
import org.opentripplanner.apis.support.TileJson;
import org.opentripplanner.apis.vectortiles.model.LayerParams;
import org.opentripplanner.apis.vectortiles.model.LayerType;
import org.opentripplanner.apis.vectortiles.model.StyleSpec;
import org.opentripplanner.apis.vectortiles.model.TileSource.VectorSource;
import org.opentripplanner.framework.io.HttpUtils;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.inspector.vector.VectorTileResponseFactory;
import org.opentripplanner.inspector.vector.edge.EdgeLayerBuilder;
import org.opentripplanner.inspector.vector.geofencing.GeofencingZonesLayerBuilder;
import org.opentripplanner.inspector.vector.stop.GroupStopLayerBuilder;
import org.opentripplanner.inspector.vector.stop.StopLayerBuilder;
import org.opentripplanner.inspector.vector.vertex.VertexLayerBuilder;
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

/**
 * Slippy map vector tile API for rendering various graph information for inspection/debugging
 * purposes.
 */
@Path("/routers/{ignoreRouterId}/inspector/vectortile")
public class GraphInspectorVectorTileResource {

  private static final LayerParams REGULAR_STOPS = new LayerParams("regularStops", RegularStop);
  private static final LayerParams AREA_STOPS = new LayerParams("areaStops", AreaStop);
  private static final LayerParams GROUP_STOPS = new LayerParams("groupStops", GroupStop);
  private static final LayerParams GEOFENCING_ZONES = new LayerParams(
    "geofencingZones",
    GeofencingZones
  );
  private static final LayerParams EDGES = new LayerParams("edges", Edge);
  private static final LayerParams VERTICES = new LayerParams("vertices", Vertex);
  private static final List<LayerParameters<LayerType>> DEBUG_LAYERS = List.of(
    REGULAR_STOPS,
    AREA_STOPS,
    GROUP_STOPS,
    GEOFENCING_ZONES,
    EDGES,
    VERTICES
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
    List<String> rlayer = Arrays.asList(requestedLayers.split(","));

    var url = TileJson.urlWithDefaultPath(
      uri,
      headers,
      rlayer,
      ignoreRouterId,
      "inspector/vectortile"
    );
    return new TileJson(url, envelope, feedInfos);
  }

  @GET
  @Path("/style.json")
  @Produces(MediaType.APPLICATION_JSON)
  public StyleSpec getTileJson(@Context UriInfo uri, @Context HttpHeaders headers) {
    var base = HttpUtils.getBaseAddress(uri, headers);

    // these could also be loaded together but are put into separate sources because
    // the stops are fast and the edges are relatively slow
    var stopsSource = new VectorSource(
      "stops",
      tileJsonUrl(base, List.of(REGULAR_STOPS, AREA_STOPS, GROUP_STOPS))
    );
    var streetSource = new VectorSource(
      "street",
      tileJsonUrl(base, List.of(EDGES, GEOFENCING_ZONES, VERTICES))
    );

    return DebugStyleSpec.build(
      REGULAR_STOPS.toVectorSourceLayer(stopsSource),
      AREA_STOPS.toVectorSourceLayer(stopsSource),
      GROUP_STOPS.toVectorSourceLayer(stopsSource),
      EDGES.toVectorSourceLayer(streetSource),
      VERTICES.toVectorSourceLayer(streetSource)
    );
  }

  private String tileJsonUrl(String base, List<LayerParameters<LayerType>> layers) {
    final String allLayers = layers
      .stream()
      .map(LayerParameters::name)
      .collect(Collectors.joining(","));
    return "%s/otp/routers/%s/inspector/vectortile/%s/tilejson.json".formatted(
        base,
        ignoreRouterId,
        allLayers
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
      case RegularStop -> new StopLayerBuilder<>(
        layerParameters,
        locale,
        e -> context.transitService().findRegularStops(e)
      );
      case AreaStop -> new StopLayerBuilder<>(
        layerParameters,
        locale,
        e -> context.transitService().findAreaStops(e)
      );
      case GroupStop -> new GroupStopLayerBuilder(
        layerParameters,
        locale,
        // There are not many GroupStops, so we can just list them all.
        context.transitService().listGroupStops()
      );
      case GeofencingZones -> new GeofencingZonesLayerBuilder(context.graph(), layerParameters);
      case Edge -> new EdgeLayerBuilder(context.graph(), layerParameters);
      case Vertex -> new VertexLayerBuilder(context.graph(), layerParameters);
    };
  }
}
