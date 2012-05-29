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
import java.util.HashSet;
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
import org.opentripplanner.api.model.transit.AgencyList;
import org.opentripplanner.api.model.transit.ModeList;
import org.opentripplanner.api.model.transit.RouteData;
import org.opentripplanner.api.model.transit.RouteList;
import org.opentripplanner.api.model.transit.ServiceCalendarData;
import org.opentripplanner.api.model.transit.Stop;
import org.opentripplanner.api.model.transit.StopList;
import org.opentripplanner.api.model.transit.StopTime;
import org.opentripplanner.api.model.transit.StopTimeList;
import org.opentripplanner.api.model.transit.TransitRoute;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
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
    private static final long MAX_STOP_TIME_QUERY_INTERVAL = 86400;

    @Autowired
    public void setGraphService(GraphService graphService) {
        this.graphService = graphService;
    }

    /**
     * Return a list of all agency ids in the graph
     */
    @GET
    @Path("/agencyIds")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getAgencyIds(@QueryParam("routerId") String routerId)
            throws JSONException {

        Graph graph = getGraph(routerId);
        
        AgencyList response = new AgencyList();
        response.agencyIds = graph.getAgencyIds();
        return response;
    }

    /**
     * Return data about a route, such as its variants and directions, that OneBusAway's API doesn't
     * handle
     */
    @GET
    @Path("/routeData")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getRouteData(@QueryParam("agency") String agency, @QueryParam("id") String id,
            @QueryParam("routerId") String routerId)
            throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
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
    public Object getRoutes(@QueryParam("agency") String agency,
            @QueryParam("routerId") String routerId)
            throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }
        Collection<AgencyAndId> allRouteIds = transitIndexService.getAllRouteIds();
        RouteList response = makeRouteList(allRouteIds, agency, routerId);
        return response;
    }

    private RouteList makeRouteList(Collection<AgencyAndId> routeIds, String agencyFilter,
            @QueryParam("routerId") String routerId) {
        RouteList response = new RouteList();
        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        for (AgencyAndId routeId : routeIds) {
            for (RouteVariant variant : transitIndexService.getVariantsForRoute(routeId)) {
                Route route = variant.getRoute();
                if (agencyFilter != null && !agencyFilter.equals(route.getAgency().getId())) continue;
                TransitRoute transitRoute = new TransitRoute();
                transitRoute.id = route.getId();
                transitRoute.routeLongName = route.getLongName();
                transitRoute.routeShortName = route.getShortName();
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
            @QueryParam("lon") Double lon,
            @QueryParam("routerId") String routerId) 
            throws JSONException {
        
        Graph graph = getGraph(routerId);

        StreetVertexIndexService streetVertexIndexService = graph.streetIndex;
        List<TransitStop> stops = streetVertexIndexService.getNearbyTransitStops(new Coordinate(lon, lat), STOP_SEARCH_RADIUS);
        TransitIndexService transitIndexService = graph.getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

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
            stop.routes = transitIndexService.getRoutesForStop(stopId);
            response.stops.add(stop);
        }

        return response;
    }

    /**
     * Return routes that a stop is served by
     */
    @GET
    @Path("/routesForStop")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getRoutesForStop(@QueryParam("agency") String agency, 
            @QueryParam("id") String id,
            @QueryParam("routerId") String routerId) 
            throws JSONException {

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        List<AgencyAndId> routes = transitIndexService.getRoutesForStop(new AgencyAndId(agency, id));
        RouteList result = makeRouteList(routes, null, routerId);

        return result;
    }

    /**
     * Return stop times for a stop, in seconds since the epoch
     */
    @GET
    @Path("/stopTimesForStop")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getStopTimesForStop(@QueryParam("agency") String stopAgency,
            @QueryParam("id") String stopId, @QueryParam("startTime") long startTime,
            @QueryParam("endTime") Long endTime,
            @QueryParam("routerId") String routerId) throws JSONException {

        if (endTime == null) {
            endTime = startTime + 86400;
        }
        if (endTime - startTime > MAX_STOP_TIME_QUERY_INTERVAL) {
            return new TransitError("Max stop time query interval is " + (endTime - startTime));
        }
        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);
        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        AgencyAndId stop = new AgencyAndId(stopAgency, stopId);
        Edge preBoardEdge = transitIndexService.getPreBoardEdge(stop);
        Vertex boarding = preBoardEdge.getToVertex();

        RoutingRequest options = makeTraverseOptions(startTime, routerId);

        //add all departures
        HashSet<AgencyAndId> trips = new HashSet<AgencyAndId>();
        StopTimeList result = new StopTimeList();
        result.stopTimes = new ArrayList<StopTime>();
        for (Edge e : boarding.getOutgoing()) {
            // each of these edges boards a separate set of trips
            for (StopTime st : getStopTimesForBoardEdge(startTime, endTime, options, e)) {
                result.stopTimes.add(st);
                trips.add(st.trip);
            }
        }

        //add the arriving stop times for cases where there are no departures
        Edge preAlightEdge = transitIndexService.getPreAlightEdge(stop);
        Vertex alighting = preAlightEdge.getFromVertex();
        for (Edge e : alighting.getIncoming()) {
            for (StopTime st : getStopTimesForAlightEdge(startTime, endTime, options, e)) {
                if (!trips.contains(st.trip)) {
                    result.stopTimes.add(st);
                }
            }
        }

        return result;
    }

    private RoutingRequest makeTraverseOptions(long startTime, String routerId) {
        RoutingRequest options = new RoutingRequest();
//        if (graphService.getCalendarService() != null) {
//            options.setCalendarService(graphService.getCalendarService());
//            options.setServiceDays(startTime, agencies);
//        }
        // TODO: verify correctness
        options.dateTime = startTime;
        options.setRoutingContext(getGraph(routerId));
        return options;
    }

    /**
     * Return subsequent stop times for a trip
     */
    @GET
    @Path("/stopTimesForTrip")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getStopTimesForTrip(@QueryParam("stopAgency") String stopAgency,
            @QueryParam("stopId") String stopId, @QueryParam("tripAgency") String tripAgency,
            @QueryParam("tripId") String tripId, @QueryParam("time") long time,
            @QueryParam("routerId") String routerId) throws JSONException {

        AgencyAndId firstStop = null;
        if (stopId != null) {
            firstStop = new AgencyAndId(stopAgency, stopId);
        }
        AgencyAndId trip = new AgencyAndId(tripAgency, tripId);

        TransitIndexService transitIndexService = getGraph(routerId).getService(
                TransitIndexService.class);

        if (transitIndexService == null) {
            return new TransitError(
                    "No transit index found.  Add TransitIndexBuilder to your graph builder configuration and rebuild your graph.");
        }

        RouteVariant variant = transitIndexService.getVariantForTrip(trip);
        RoutingRequest options = makeTraverseOptions(time, routerId);

        StopTimeList result = new StopTimeList();
        result.stopTimes = new ArrayList<StopTime>();
        State state = null;
        RouteSegment start = null;
        for (RouteSegment segment : variant.getSegments()) {
            //this is all segments across all patterns that match this variant
            if (segment.stop.equals(firstStop)) {
                //this might be the correct start segment, but we need to try traversing and see if we get this trip
                // TODO: verify options and state creation correctness (AMB)
                State s0 = new State(segment.board.getFromVertex(), options);
                state = segment.board.traverse(s0);
                if (state == null) continue;
                if (state.getBackEdgeNarrative().getTrip().getId().equals(trip)) {
                    start = segment;
                    StopTime st = new StopTime();
                    st.time = state.getTime();
                    st.stop = segment.stop;
                    result.stopTimes.add(st);
                    break;
                }
            }
        }
        if (start == null) {
            return null;
        }

        for (RouteSegment segment :  variant.segmentsAfter(start)) {
            // TODO: verify options/state init correctness
            State s0 = new State(segment.hopIn.getFromVertex(), state.getTime(), options);
            state = segment.hopIn.traverse(s0);
            StopTime st = new StopTime();
            st.time = state.getTime();
            st.stop = segment.stop;
            result.stopTimes.add(st);
        }
        return result;
    }

    private List<StopTime> getStopTimesForBoardEdge(long startTime, long endTime,
            RoutingRequest options, Edge e) {
        List<StopTime> out = new ArrayList<StopTime>();
        State result;
        long time = startTime;
        do {
            // TODO verify options/state correctness
            State s0 = new State(e.getFromVertex(), time, options);
            result = e.traverse(s0);
            if (result == null) break;
            time = result.getTime();
            if (time > endTime)
                break;
            StopTime stopTime = new StopTime();
            stopTime.time = time;
            stopTime.trip = result.getBackEdgeNarrative().getTrip().getId();
            out.add(stopTime);

            time += 1; // move to the next board time
        } while (true);
        return out;
    }

    private List<StopTime> getStopTimesForAlightEdge(long startTime, long endTime,
            RoutingRequest options, Edge e) {
        List<StopTime> out = new ArrayList<StopTime>();
        State result;
        long time = endTime;
        options = options.reversedClone();
        do {
            // TODO: verify options/state correctness
            State s0 = new State(e.getToVertex(), time, options);
            result = e.traverse(s0);
            if (result == null) break;
            time = result.getTime();
            if (time < startTime)
                break;
            StopTime stopTime = new StopTime();
            stopTime.time = time;
            stopTime.trip = result.getBackEdgeNarrative().getTrip().getId();
            out.add(stopTime);
            time -= 1; // move to the previous alight time
        } while (true);
        return out;
    }
    
    /**
     * Return a list of all available transit modes supported, if any.
     * 
     * @throws JSONException
     */
    @GET
    @Path("/modes")
    @Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.TEXT_XML })
    public Object getModes(
            @QueryParam("routerId") String routerId) throws JSONException {
        TransitIndexService transitIndexService = getGraph(routerId).getService(
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

    private Graph getGraph(String routerId) {
        return graphService.getGraph(routerId);
    }

    public Object getCalendarServiceDataForAgency(@QueryParam("agency") String agency,
            @QueryParam("routerId") String routerId) {
        TransitIndexService transitIndexService = getGraph(routerId).getService(
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
