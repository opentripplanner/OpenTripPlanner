package org.opentripplanner.ext.vectortiles;

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
import java.util.Set;
import java.util.function.Predicate;
import org.glassfish.grizzly.http.server.Request;
import org.opentripplanner.apis.support.TileJson;
import org.opentripplanner.ext.vectortiles.layers.areastops.AreaStopsLayerBuilder;
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
import org.opentripplanner.model.FeedInfo;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

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
      serverContext.vectorTileConfig().layers(),
      VectorTilesResource::createLayerBuilder,
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

    List<String> rLayers = Arrays.asList(requestedLayers.split(","));

    var config = serverContext.vectorTileConfig();
    var url = config
      .basePath()
      .map(overrideBasePath ->
        TileJson.urlFromOverriddenBasePath(uri, headers, overrideBasePath, rLayers)
      )
      .orElseGet(() ->
        TileJson.urlWithDefaultPath(uri, headers, rLayers, ignoreRouterId, "vectorTiles")
      );

    int minZoom = config.minZoom(Set.copyOf(rLayers));
    int maxZoom = config.maxZoom(Set.copyOf(rLayers));
    return config
      .attribution()
      .map(attr -> new TileJson(url, envelope, attr, minZoom, maxZoom))
      .orElseGet(() -> {
        var feedInfos = getFeedInfos();
        return new TileJson(url, envelope, feedInfos, minZoom, maxZoom);
      });
  }

  private List<FeedInfo> getFeedInfos() {
    return serverContext
      .transitService()
      .listFeedIds()
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
      case Stop -> new StopsLayerBuilder(context.transitService(), layerParameters, locale);
      case Station -> new StationsLayerBuilder(context.transitService(), layerParameters, locale);
      case AreaStop -> new AreaStopsLayerBuilder(context.transitService(), layerParameters, locale);
      case VehicleRental -> new VehicleRentalPlacesLayerBuilder(
        context.vehicleRentalService(),
        layerParameters,
        locale
      );
      case VehicleRentalStation -> new VehicleRentalStationsLayerBuilder(
        context.vehicleRentalService(),
        layerParameters,
        locale
      );
      case VehicleRentalVehicle -> new VehicleRentalVehiclesLayerBuilder(
        context.vehicleRentalService(),
        layerParameters
      );
      case VehicleParking -> new VehicleParkingsLayerBuilder(
        context.vehicleParkingService(),
        layerParameters,
        locale
      );
      case VehicleParkingGroup -> new VehicleParkingGroupsLayerBuilder(
        context.vehicleParkingService(),
        layerParameters,
        locale
      );
    };
  }

  public enum LayerType {
    Stop,
    Station,
    AreaStop,
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
