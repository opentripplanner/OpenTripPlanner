package org.opentripplanner.api.resource;

import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
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
    public BikeRentalStationList getBikeRentalStations(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight,
            @QueryParam("locale") String locale_param) {

        Router router = otpServer.getRouter();

        BikeRentalStationService bikeRentalService = router.graph.getService(BikeRentalStationService.class);
        Locale locale;
        locale = ResourceBundleSingleton.INSTANCE.getLocale(locale_param);
        if (bikeRentalService == null) return new BikeRentalStationList();
        Envelope envelope;
        if (lowerLeft != null) {
            envelope = getEnvelope(lowerLeft, upperRight);
        } else {
            envelope = new Envelope(-180,180,-90,90); 
        }
        Collection<BikeRentalStation> stations = bikeRentalService.getBikeRentalStations();
        List<BikeRentalStation> out = new ArrayList<>();
        for (BikeRentalStation station : stations) {
            if (envelope.contains(station.x, station.y)) {
                BikeRentalStation station_localized = station.clone();
                station_localized.locale = locale;
                out.add(station_localized);
            }
        }
        BikeRentalStationList brsl = new BikeRentalStationList();
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
