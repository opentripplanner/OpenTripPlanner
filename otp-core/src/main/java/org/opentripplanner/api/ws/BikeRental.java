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

package org.opentripplanner.api.ws;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.opentripplanner.api.ws.internals.GraphInternals;
import org.opentripplanner.routing.bike_rental.BikeRentalStation;
import org.opentripplanner.routing.bike_rental.BikeRentalStationService;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Envelope;
import org.opentripplanner.util.ResourceBundleSingleton;

@Path("/bike_rental")
@XmlRootElement
@Autowire
public class BikeRental {
    private GraphService graphService;

    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }
    
    @GET
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public BikeRentalStationList getBikeRentalStations(
            @QueryParam("lowerLeft") String lowerLeft,
            @QueryParam("upperRight") String upperRight,
            @QueryParam("routerId") String routerId,
            @QueryParam("locale") String locale_param) {

        Graph graph = graphService.getGraph(routerId);
        if (graph == null) return null;
        Locale locale;
        locale = ResourceBundleSingleton.INSTANCE.getLocale(locale_param);
        ResourceBundle resources_names = ResourceBundle.getBundle("WayProperties", locale);
        BikeRentalStationService bikeRentalService = graph.getService(BikeRentalStationService.class);
        if (bikeRentalService == null) return new BikeRentalStationList();
        Envelope envelope;
        if (lowerLeft != null) {
            envelope = GraphInternals.getEnvelope(lowerLeft, upperRight);
        } else {
            envelope = new Envelope(-180,180,-90,90); 
        }
        Collection<BikeRentalStation> stations = bikeRentalService.getStations();
        List<BikeRentalStation> out = new ArrayList<BikeRentalStation>();
        for (BikeRentalStation station : stations) {
            if (envelope.contains(station.x, station.y)) {
                if (station.raw_name != null) {
                    station.name = station.raw_name.toString(locale);
                }
                out.add(station);
            }
        }
        BikeRentalStationList brsl = new BikeRentalStationList();
        brsl.stations = out;
        return brsl;
    }
    
    private String localize(String key, ResourceBundle resourceBundle) {
        if (key == null) {
            return null;
        }
        try {
            String retval = resourceBundle.getString(key);
            //LOG.debug(String.format("Localized '%s' using '%s'", key, retval));
            return retval;
        } catch (MissingResourceException e) {
            //LOG.debug("Missing translation for key: " + key);
            return key;
        }
    }

}
