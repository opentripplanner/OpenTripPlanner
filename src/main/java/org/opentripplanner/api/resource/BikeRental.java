/* This program is free software: you can redistribute it and/or
 modify it under the terms of the GNU Lesser General Public License
 as published by the Free Software Foundation, either version 3 of
 the License, or (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>. */

package org.opentripplanner.api.resource;

import static org.opentripplanner.api.resource.ServerInfo.Q;

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
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;

import com.vividsolutions.jts.geom.Envelope;
import org.opentripplanner.util.ResourceBundleSingleton;

@Path("/routers/{routerId}/bike_rental")
@XmlRootElement
public class BikeRental {

    @Context
    OTPServer otpServer;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public BikeRentalStationList getBikeRentalStations(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight,
            @PathParam("routerId") String routerId,
            @QueryParam("locale") String locale_param) {

        Router router = otpServer.getRouter(routerId);
        if (router == null) return null;
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
