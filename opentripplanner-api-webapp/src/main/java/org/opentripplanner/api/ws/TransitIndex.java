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
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jettison.json.JSONException;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.opentripplanner.api.model.error.TransitError;
import org.opentripplanner.api.model.transit.RouteList;
import org.opentripplanner.api.model.transit.ServiceCalendarData;
import org.opentripplanner.api.model.transit.ModeList;
import org.opentripplanner.api.model.transit.RouteData;
import org.opentripplanner.api.model.transit.Stop;
import org.opentripplanner.api.model.transit.StopList;
import org.opentripplanner.api.model.transit.TransitRoute;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.springframework.beans.factory.annotation.Autowired;

import com.sun.jersey.api.spring.Autowire;
import com.vividsolutions.jts.geom.Coordinate;

// NOTE - /ws/transit is the full path -- see web.xml

@Path("/transit")
@XmlRootElement
@Autowire
public class TransitIndex {

    private static final double STOP_SEARCH_RADIUS = 200;
    private GraphService graphService;
    private StreetVertexIndexService streetVertexIndexService;

    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    @Autowired
    public void setIndexService(StreetVertexIndexService indexService) {
        this.streetVertexIndexService = indexService;
    }

    /**
     * Return data about a route, such as its variants and directions, that OneBusAway's API doesn't
     * handle
     */
    @GET
    @Path("/routeData")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getRouteData(@QueryParam("agency") String agency, @QueryParam("id") String id)
            throws JSONException {

        TransitIndexService transitIndexService = graphService.getGraph().getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        RouteData response = new RouteData();
        AgencyAndId routeId = new AgencyAndId(agency, id);
        response.id = routeId;
        List<RouteVariant> variants = transitIndexService.getVariantsForRoute(routeId);

        response.variants = variants;
        response.directions = new ArrayList<String>(
                transitIndexService.getDirectionsForRoute(routeId));

        return response;
    }
    /**
     * Return a list of route ids
     */
    @GET
    @Path("/routes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getRoutes(@QueryParam("agency") String agency)
            throws JSONException {

        TransitIndexService transitIndexService = graphService.getGraph().getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        RouteList response = new RouteList();
        for (AgencyAndId routeId : transitIndexService.getAllRouteIds()) {
            for (RouteVariant variant : transitIndexService.getVariantsForRoute(routeId)) {
                Route route = variant.getRoute();
                if (agency != null && !agency.equals(route.getAgency().getId())) continue;
                TransitRoute transitRoute = new TransitRoute();
                transitRoute.id = route.getId();
                transitRoute.routeLongName = route.getLongName();
                transitRoute.routeShortName = route.getLongName();
                transitRoute.url = route.getUrl();
                response.routes.add(transitRoute);
                break;
            }
        }
        return response;
    }

    /**
     * Return stops near a point
     */
    @GET
    @Path("/stopsNearPoint")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getStopsNearPoint(@QueryParam("agency") String agency, 
            @QueryParam("lat") Double lat,
            @QueryParam("lon") Double lon) 
            throws JSONException {
        
        List<TransitStop> stops = streetVertexIndexService.getNearbyTransitStops(new Coordinate(lon, lat), STOP_SEARCH_RADIUS);
        
        StopList response = new StopList();
        for (TransitStop transitStop : stops) {
            AgencyAndId stopId = transitStop.getStopId();
            if (agency != null && !agency.equals(stopId.getAgencyId())) continue;
            Stop stop = new Stop();
            stop.id = stopId;
            stop.lat = transitStop.getLat();
            stop.lon = transitStop.getLon();
            stop.stopCode = transitStop.getStopCode();
            stop.stopName = transitStop.getName();
            response.stops.add(stop);
        }
        
        return response;
    }

    /**
     * Return a list of all available transit modes supported, if any.
     * 
     * @throws JSONException
     */
    @GET
    @Path("/modes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getModes() throws JSONException {
        TransitIndexService transitIndexService = graphService.getGraph().getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        ModeList modes = new ModeList();
        modes.modes = new ArrayList<TraverseMode>();
        for (TraverseMode mode : transitIndexService.getAllModes()) {
            modes.modes.add(mode);
        }
        return modes;
    }

    public Object getCalendarServiceDataForAgency(@QueryParam("agency") String agency) {
        TransitIndexService transitIndexService = graphService.getGraph().getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        ServiceCalendarData data = new ServiceCalendarData();

        data.calendars = transitIndexService.getCalendarsByAgency(agency);
        data.calendarDates = transitIndexService.getCalendarDatesByAgency(agency);

        return data;
    }

}
