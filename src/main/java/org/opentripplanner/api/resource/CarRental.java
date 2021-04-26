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

import org.locationtech.jts.geom.Envelope;
import org.opentripplanner.routing.car_rental.CarRentalStation;
import org.opentripplanner.routing.car_rental.CarRentalStationService;
import org.opentripplanner.standalone.OTPServer;
import org.opentripplanner.standalone.Router;
import org.opentripplanner.util.ResourceBundleSingleton;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;

import static org.opentripplanner.api.resource.ServerInfo.Q;

@Path("/routers/{routerId}/car_rental")
@XmlRootElement
public class CarRental {

    @Context
    OTPServer otpServer;

    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML + Q, MediaType.TEXT_XML + Q })
    public CarRentalStationList getCarRentalStations(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight,
            @PathParam("routerId") String routerId,
            @QueryParam("locale") String locale_param,
            @QueryParam("company") String company
    ) {
        Router router = otpServer.getRouter(routerId);
        if (router == null) return null;
        CarRentalStationService carRentalService = router.graph.getService(CarRentalStationService.class);
        Locale locale = ResourceBundleSingleton.INSTANCE.getLocale(locale_param);
        if (carRentalService == null) return new CarRentalStationList();
        Envelope envelope;
        if (lowerLeft != null) {
            envelope = getEnvelope(lowerLeft, upperRight);
        } else {
            envelope = new Envelope(-180,180,-90,90); 
        }
        Collection<CarRentalStation> stations = carRentalService.getCarRentalStations();
        List<CarRentalStation> out = new LinkedList<>();
        for (CarRentalStation station : stations) {
            if (envelope.contains(station.x, station.y) &&
                (station.x != 0 && station.y != 0) &&
                !station.isBorderDropoff &&
                (company == null || station.networks.contains(company))
            ) {
                CarRentalStation station_localized = station.clone();
                station_localized.locale = locale;
                out.add(station_localized);
            }
        }
        CarRentalStationList carRentalStationList = new CarRentalStationList();
        carRentalStationList.stations = out;
        carRentalStationList.errorsByNetwork = carRentalService.getErrorsByNetwork();
        carRentalStationList.systemInformationDataByNetwork = carRentalService.getSystemInformationDataByNetwork();
        return carRentalStationList;
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
