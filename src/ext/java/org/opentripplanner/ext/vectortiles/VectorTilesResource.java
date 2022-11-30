package org.opentripplanner.ext.vectortiles;

import edu.colorado.cires.cmg.mvt.VectorTile;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
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
import org.geotools.geometry.Envelope2D;
import org.glassfish.grizzly.http.server.Request;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.model.TileJson;
import org.opentripplanner.api.resource.WebMercatorTile;
import org.opentripplanner.ext.vectortiles.layers.stations.StationsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.stops.StopsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehicleparkings.VehicleParkingGroupsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehicleparkings.VehicleParkingsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.VehicleRentalPlacesLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.VehicleRentalStationsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.VehicleRentalVehiclesLayerBuilder;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

@Path("/routers/{ignoreRouterId}/vectorTiles")
public class VectorTilesResource {

  public static final String APPLICATION_X_PROTOBUF = "application/x-protobuf";

  private final OtpServerRequestContext serverContext;
  private final String ignoreRouterId;
  private final Locale locale;

  public VectorTilesResource(
    @Context OtpServerRequestContext serverContext,
    @Context Request grizzlyRequest,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.locale = grizzlyRequest.getLocale();
    this.serverContext = serverContext;
    this.ignoreRouterId = ignoreRouterId;
  }

  @GET
  @Path("/{layers}/{z}/{x}/{y}.pbf")
  @Produces(APPLICATION_X_PROTOBUF)
  public Response tileGet(
    @PathParam("x") int x,
    @PathParam("y") int y,
    @PathParam("z") int z,
    @PathParam("layers") String requestedLayers
  ) {
    VectorTile.Tile.Builder mvtBuilder = VectorTile.Tile.newBuilder();

    if (z < LayerParameters.MIN_ZOOM) {
      return Response.status(Response.Status.OK).entity(mvtBuilder.build().toByteArray()).build();
    }

    Envelope2D env = WebMercatorTile.tile2Envelope(x, y, z);
    Envelope envelope = new Envelope(env.getMaxX(), env.getMinX(), env.getMaxY(), env.getMinY());

    List<String> layers = Arrays.asList(requestedLayers.split(","));

    int cacheMaxSeconds = Integer.MAX_VALUE;

    for (LayerParameters<LayerType> layerParameters : serverContext.vectorTileLayers().layers()) {
      if (
        layers.contains(layerParameters.name()) &&
        layerParameters.minZoom() <= z &&
        z <= layerParameters.maxZoom()
      ) {
        cacheMaxSeconds = Math.min(cacheMaxSeconds, layerParameters.cacheMaxSeconds());
        VectorTile.Tile.Layer layer = crateLayerBuilder(
          layerParameters.type(),
          serverContext.graph(),
          serverContext.transitService(),
          layerParameters,
          locale
        )
          .build(envelope);
        mvtBuilder.addLayers(layer);
      }
    }

    CacheControl cacheControl = new CacheControl();
    if (cacheMaxSeconds != Integer.MAX_VALUE) {
      cacheControl.setMaxAge(cacheMaxSeconds);
    }
    byte[] bytes = mvtBuilder.build().toByteArray();
    return Response.status(Response.Status.OK).cacheControl(cacheControl).entity(bytes).build();
  }

  @GET
  @Path("/{layers}/tilejson.json")
  @Produces(MediaType.APPLICATION_JSON)
  public TileJson getTileJson(
    @Context UriInfo uri,
    @Context HttpHeaders headers,
    @PathParam("layers") String requestedLayers
  ) {
    return new TileJson(
      uri,
      headers,
      requestedLayers,
      ignoreRouterId,
      "vectorTiles",
      serverContext.graph().getEnvelope(),
      serverContext
        .transitService()
        .getFeedIds()
        .stream()
        .map(serverContext.transitService()::getFeedInfo)
        .filter(Predicate.not(Objects::isNull))
        .toList(),
      serverContext.transitService().getCenter().orElse(null)
    );
  }

  private static LayerBuilder<?> crateLayerBuilder(
    LayerType layerType,
    Graph graph,
    TransitService transitService,
    LayerParameters<LayerType> layerParameters,
    Locale locale
  ) {
    return switch (layerType) {
      case Stop -> new StopsLayerBuilder(transitService, layerParameters, locale);
      case Station -> new StationsLayerBuilder(transitService, layerParameters, locale);
      case VehicleRental -> new VehicleRentalPlacesLayerBuilder(
        graph.getVehicleRentalService(),
        layerParameters,
        locale
      );
      case VehicleRentalStation -> new VehicleRentalStationsLayerBuilder(
        graph.getVehicleRentalService(),
        layerParameters,
        locale
      );
      case VehicleRentalVehicle -> new VehicleRentalVehiclesLayerBuilder(
        graph.getVehicleRentalService(),
        layerParameters
      );
      case VehicleParking -> new VehicleParkingsLayerBuilder(graph, layerParameters, locale);
      case VehicleParkingGroup -> new VehicleParkingGroupsLayerBuilder(
        graph,
        layerParameters,
        locale
      );
    };
  }

  public enum LayerType {
    Stop,
    Station,
    VehicleRental,
    VehicleRentalVehicle,
    VehicleRentalStation,
    VehicleParking,
    VehicleParkingGroup,
  }

  public interface LayersParameters<T extends Enum<T>> {
    List<LayerParameters<T>> layers();
  }
}
