package org.opentripplanner.ext.parkAndRideApi;

import java.util.List;
import java.util.Optional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.routing.impl.StreetVertexIndex;
import org.opentripplanner.routing.vehicle_parking.VehicleParking;
import org.opentripplanner.routing.vehicle_parking.VehicleParkingService;
import org.opentripplanner.routing.vertextype.TransitStopVertex;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.util.I18NString;

/**
 * Created by demory on 7/26/18.
 */

@Path("/routers/{ignoreRouterId}/park_and_ride")
public class ParkAndRideResource {

  private final VehicleParkingService vehicleParkingService;
  private final StreetVertexIndex streetIndex;

  public ParkAndRideResource(
    @Context OTPServer otpServer,
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId
  ) {
    this.vehicleParkingService = otpServer.getRouter().graph.getVehicleParkingService();
    this.streetIndex = otpServer.getRouter().graph.getStreetIndex();
  }

  /** Envelopes are in latitude, longitude format */
  public static Envelope getEnvelope(String lowerLeft, String upperRight) {
    String[] lowerLeftParts = lowerLeft.split(",");
    String[] upperRightParts = upperRight.split(",");

    return new Envelope(
      Double.parseDouble(lowerLeftParts[1]),
      Double.parseDouble(upperRightParts[1]),
      Double.parseDouble(lowerLeftParts[0]),
      Double.parseDouble(upperRightParts[0])
    );
  }

  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getParkAndRide(
    @QueryParam("lowerLeft") String lowerLeft,
    @QueryParam("upperRight") String upperRight,
    @QueryParam("maxTransitDistance") Double maxTransitDistance
  ) {
    Envelope envelope;
    if (lowerLeft != null) {
      envelope = getEnvelope(lowerLeft, upperRight);
    } else {
      envelope = new Envelope(-180, 180, -90, 90);
    }

    var prs = vehicleParkingService
      .getCarParks()
      .filter(lot -> envelope.contains(new Coordinate(lot.getX(), lot.getY())))
      .filter(lot -> hasTransitStopsNearby(maxTransitDistance, lot))
      .map(ParkAndRideInfo::ofVehicleParking)
      .toList();

    return Response.status(Status.OK).entity(prs).build();
  }

  private boolean hasTransitStopsNearby(Double maxTransitDistance, VehicleParking lot) {
    if (maxTransitDistance == null) {
      return true;
    } else {
      List<TransitStopVertex> stops = streetIndex.getNearbyTransitStops(
        new Coordinate(lot.getX(), lot.getY()),
        maxTransitDistance
      );
      return !stops.isEmpty();
    }
  }

  public record ParkAndRideInfo(String name, double x, double y) {
    public static ParkAndRideInfo ofVehicleParking(VehicleParking parking) {
      var name = Optional
        .ofNullable(parking.getName())
        .map(I18NString::toString)
        .orElse(parking.getId().getId());

      return new ParkAndRideInfo(name, parking.getX(), parking.getY());
    }
  }
}
