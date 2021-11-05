package org.opentripplanner.api.resource;

import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.api.mapping.VehicleRentalStationMapper;
import org.opentripplanner.api.model.ApiVehicleRentalStation;
import org.opentripplanner.api.model.ApiVehicleRentalStationList;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalPlace;
import org.opentripplanner.routing.vehicle_rental.VehicleRentalStationService;
import org.opentripplanner.standalone.server.OTPServer;
import org.opentripplanner.standalone.server.Router;
import org.opentripplanner.util.ResourceBundleSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Path("/routers/{ignoreRouterId}/bike_rental")
public class BikeRental {
    /**
     * @deprecated The support for multiple routers are removed from OTP2.
     * See https://github.com/opentripplanner/OpenTripPlanner/issues/2760
     */
    @Deprecated @PathParam("ignoreRouterId")
    private String ignoreRouterId;

    @Context
    OTPServer otpServer;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public ApiVehicleRentalStationList getBikeRentalStations(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight,
            @QueryParam("locale") String locale_param) {

        Router router = otpServer.getRouter();

        VehicleRentalStationService vehicleRentalService = router.graph.getService(VehicleRentalStationService.class);
        Locale locale;
        locale = ResourceBundleSingleton.INSTANCE.getLocale(locale_param);
        if (vehicleRentalService == null) { return new ApiVehicleRentalStationList(); }
        Envelope envelope;
        if (lowerLeft != null) {
            envelope = getEnvelope(lowerLeft, upperRight);
        } else {
            envelope = new Envelope(-180,180,-90,90); 
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

    /** Envelopes are in latitude, longitude format */
    public static Envelope getEnvelope(String lowerLeft, String upperRight) {
        String[] lowerLeftParts = lowerLeft.split(",");
        String[] upperRightParts = upperRight.split(",");

        Envelope envelope = new Envelope(Double.parseDouble(lowerLeftParts[1]),
                Double.parseDouble(upperRightParts[1]), Double.parseDouble(lowerLeftParts[0]),
                Double.parseDouble(upperRightParts[0]));
        return envelope;
    }

}
