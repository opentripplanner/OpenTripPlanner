package org.opentripplanner.ext.vectortiles;

import static org.opentripplanner.framework.io.HttpUtils.APPLICATION_X_PROTOBUF;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Predicate;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.glassfish.grizzly.http.server.Request;
import org.opentripplanner.api.model.TileJson;
import org.opentripplanner.ext.vectortiles.layers.stations.StationsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.stops.StopsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehicleparkings.VehicleParkingGroupsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehicleparkings.VehicleParkingsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.VehicleRentalPlacesLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.VehicleRentalStationsLayerBuilder;
import org.opentripplanner.ext.vectortiles.layers.vehiclerental.VehicleRentalVehiclesLayerBuilder;
import org.opentripplanner.inspector.vector.LayerBuilder;
import org.opentripplanner.inspector.vector.LayerParameters;
import org.opentripplanner.inspector.vector.VectorTileResponseFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.standalone.api.OtpServerRequestContext;
import org.opentripplanner.transit.service.TransitService;

@Path("/routers/{ignoreRouterId}/vectorTiles")
public class VectorTilesResource {

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
    return VectorTileResponseFactory.create(
      x,
      y,
      z,
      locale,
      Arrays.asList(requestedLayers.split(",")),
      serverContext.vectorTileLayers().layers(),
      VectorTilesResource::crateLayerBuilder,
      serverContext.graph(),
      serverContext.transitService()
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
    var feedInfos = serverContext
      .transitService()
      .getFeedIds()
      .stream()
      .map(serverContext.transitService()::getFeedInfo)
      .filter(Predicate.not(Objects::isNull))
      .toList();

    return new TileJson(
      uri,
      headers,
      requestedLayers,
      ignoreRouterId,
      "vectorTiles",
      envelope,
      feedInfos
    );
  }

  private static LayerBuilder<?> crateLayerBuilder(
    LayerParameters<LayerType> layerParameters,
    Locale locale,
    Graph graph,
    TransitService transitService
  ) {
    return switch (layerParameters.type()) {
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
