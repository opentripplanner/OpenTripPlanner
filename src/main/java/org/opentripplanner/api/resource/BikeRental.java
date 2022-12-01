package org.opentripplanner.api.resource;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.mapping.VehicleRentalStationMapper;
import org.opentripplanner.api.model.ApiVehicleRentalStation;
import org.opentripplanner.api.model.ApiVehicleRentalStationList;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalService;
import org.opentripplanner.standalone.api.OtpServerRequestContext;

@Path("/routers/{ignoreRouterId}/bike_rental")
public class BikeRental {

  private final OtpServerRequestContext serverContext;

  public BikeRental(
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId") String ignoreRouterId,
    @Context OtpServerRequestContext serverContext
  ) {
    this.serverContext = serverContext;
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
  public ApiVehicleRentalStationList getBikeRentalStations(
    @QueryParam("lowerLeft") String lowerLeft,
    @QueryParam("upperRight") String upperRight,
    @QueryParam("locale") String locale_param
  ) {
    OtpServerRequestContext serverContext = this.serverContext;

    VehicleRentalService vehicleRentalService = serverContext.graph().getVehicleRentalService();
    Locale locale = locale_param != null && !locale_param.isBlank()
      ? Locale.forLanguageTag(locale_param.replaceAll("-", "_"))
      : Locale.ENGLISH;
    if (vehicleRentalService == null) {
      return new ApiVehicleRentalStationList();
    }
    Envelope envelope;
    if (lowerLeft != null) {
      envelope = getEnvelope(lowerLeft, upperRight);
    } else {
      envelope = new Envelope(-180, 180, -90, 90);
    }
    Collection<VehicleRentalPlace> stations = vehicleRentalService.getVehicleRentalPlaces();
    List<ApiVehicleRentalStation> out = new ArrayList<>();
    for (VehicleRentalPlace station : stations) {
      if (envelope.contains(station.getLongitude(), station.getLatitude())) {
        out.add(VehicleRentalStationMapper.mapToApi(station, locale));
      }
    }
    ApiVehicleRentalStationList brsl = new ApiVehicleRentalStationList();
    brsl.stations = out;
    return brsl;
  }
}
