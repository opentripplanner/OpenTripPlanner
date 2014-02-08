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

package org.opentripplanner.routing.transit_index;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.vividsolutions.jts.geom.Coordinate;
import org.onebusaway.gtfs.model.*;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.*;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.util.MapUtils;

import java.io.Serializable;
import java.util.*;

public class TransitIndexServiceImpl implements TransitIndexService, Serializable {
    private static final long serialVersionUID = -8147894489513820239L;

    private HashMap<String, List<RouteVariant>> variantsByAgency;

    private HashMap<AgencyAndId, List<RouteVariant>> variantsByRoute;

    private HashMap<AgencyAndId, RouteVariant> variantsByTrip;

    private HashMap<AgencyAndId, PreAlightEdge> preAlightEdges;

    private HashMap<AgencyAndId, PreBoardEdge> preBoardEdges;

    private HashMap<AgencyAndId, TableTripPattern> tableTripPatternsByTrip;

    private HashMap<AgencyAndId, HashSet<String>> directionsForRoute;

    private HashMap<AgencyAndId, HashSet<Stop>> stopsForRoute;

    private List<TraverseMode> modes;

    private ListMultimap<String, ServiceCalendar> calendarsByAgency = LinkedListMultimap.create();

    private ListMultimap<String, ServiceCalendarDate> calendarDatesByAgency = LinkedListMultimap.create();

    private HashMap<String, Agency> agencies = new HashMap<String, Agency>();

    private HashMap<AgencyAndId, Stop> stops = new HashMap<AgencyAndId, Stop>();

    private HashMap<AgencyAndId, Route> routes = new HashMap<AgencyAndId, Route>();

    private Coordinate center;

    private int overnightBreak;

    public TransitIndexServiceImpl(HashMap<String, List<RouteVariant>> variantsByAgency,
                                   HashMap<AgencyAndId, List<RouteVariant>> variantsByRoute,
                                   HashMap<AgencyAndId, RouteVariant> variantsByTrip,
                                   HashMap<AgencyAndId, PreBoardEdge> preBoardEdges,
                                   HashMap<AgencyAndId, PreAlightEdge> preAlightEdges,
                                   HashMap<AgencyAndId, TableTripPattern> tableTripPatternsByTrip,
                                   HashMap<AgencyAndId, HashSet<String>> directionsByRoute,
                                   HashMap<AgencyAndId, HashSet<Stop>> stopsByRoute,
                                   HashMap<AgencyAndId, Route> routes,
                                   HashMap<AgencyAndId, Stop> stops,
                                   List<TraverseMode> modes) {
        this.variantsByAgency = variantsByAgency;
        this.variantsByRoute = variantsByRoute;
        this.variantsByTrip = variantsByTrip;
        this.preBoardEdges = preBoardEdges;
        this.preAlightEdges = preAlightEdges;
        this.tableTripPatternsByTrip = tableTripPatternsByTrip;
        this.directionsForRoute = directionsByRoute;
        this.stopsForRoute = stopsByRoute;
        this.routes = routes;
        this.stops = stops;
        this.modes = modes;
    }

    public void merge(HashMap<String, List<RouteVariant>> variantsByAgency,
                      HashMap<AgencyAndId, List<RouteVariant>> variantsByRoute,
                      HashMap<AgencyAndId, RouteVariant> variantsByTrip,
                      HashMap<AgencyAndId, PreBoardEdge> preBoardEdges,
                      HashMap<AgencyAndId, PreAlightEdge> preAlightEdges,
                      HashMap<AgencyAndId, TableTripPattern> tableTripPatternsByTrip,
                      HashMap<AgencyAndId, HashSet<String>> directionsByRoute,
                      HashMap<AgencyAndId, HashSet<Stop>> stopsByRoute,
                      HashMap<AgencyAndId, Route> routes,
                      HashMap<AgencyAndId, Stop> stops,
                      List<TraverseMode> modes) {

        MapUtils.mergeInUnique(this.variantsByAgency, variantsByAgency);
        MapUtils.mergeInUnique(this.variantsByRoute, variantsByRoute);
        this.variantsByTrip.putAll(variantsByTrip);
        this.preBoardEdges.putAll(preBoardEdges);
        this.preAlightEdges.putAll(preAlightEdges);
        this.tableTripPatternsByTrip.putAll(tableTripPatternsByTrip);
        MapUtils.mergeInUnique(this.directionsForRoute, directionsByRoute);
        MapUtils.mergeInUnique(this.stopsForRoute, stopsByRoute);
        this.routes.putAll(routes);
        this.stops.putAll(stops);
        for (TraverseMode mode : modes) {
            if (!this.modes.contains(mode)) {
                this.modes.add(mode);
            }
        }
    }

    @Override
    public List<RouteVariant> getVariantsForAgency(String agency) {
        List<RouteVariant> variants = variantsByAgency.get(agency);
        if (variants == null) {
            return Collections.emptyList();
        }
        return variants;
    }

    @Override
    public List<RouteVariant> getVariantsForRoute(AgencyAndId route) {
        List<RouteVariant> variants = variantsByRoute.get(route);
        if (variants == null) {
            return Collections.emptyList();
        }
        return variants;
    }

    @Override
    public RouteVariant getVariantForTrip(AgencyAndId trip) {
        return variantsByTrip.get(trip);
    }

    @Override
    public PreAlightEdge getPreAlightEdge(AgencyAndId stop) {
        return preAlightEdges.get(stop);
    }

    @Override
    public TableTripPattern getTripPatternForTrip(AgencyAndId tripId) {
        return tableTripPatternsByTrip.get(tripId);
    }

    @Override
    public PreBoardEdge getPreBoardEdge(AgencyAndId stop) {
        return preBoardEdges.get(stop);
    }

    @Override
    public Collection<String> getDirectionsForRoute(AgencyAndId route) {
        return directionsForRoute.get(route);
    }

    @Override
    public List<TraverseMode> getAllModes() {
        return modes;
    }

    @Override
    public List<String> getAllAgencies() {
        return new ArrayList<String>(variantsByAgency.keySet());
    }

    @Override
    public Collection<AgencyAndId> getAllRouteIds() {
        return variantsByRoute.keySet();
    }

    @Override
    public Map<AgencyAndId, Route> getAllRoutes() {
        return Collections.unmodifiableMap(routes);
    }

    @Override
    public Map<AgencyAndId, Stop> getAllStops() {
        return Collections.unmodifiableMap(stops);
    }

    @Override
    public void addCalendars(Collection<ServiceCalendar> allCalendars) {
        for (ServiceCalendar calendar : allCalendars) {
            calendarsByAgency.put(calendar.getServiceId().getAgencyId(), calendar);
        }
    }

    @Override
    public void addCalendarDates(Collection<ServiceCalendarDate> allDates) {
        for (ServiceCalendarDate date : allDates) {
            calendarDatesByAgency.put(date.getServiceId().getAgencyId(), date);
        }
    }

    @Override
    public List<ServiceCalendarDate> getCalendarDatesByAgency(String agency) {
        return calendarDatesByAgency.get(agency);
    }

    @Override
    public List<ServiceCalendar> getCalendarsByAgency(String agency) {
        return calendarsByAgency.get(agency);
    }

    @Override
    public Agency getAgency(String id) {
        return agencies.get(id);
    }

    public void addAgency(Agency agency) {
        agencies.put(agency.getId(), agency);
    }

    @Override
    public List<AgencyAndId> getRoutesForStop(AgencyAndId stop) {
        HashSet<AgencyAndId> out = new HashSet<AgencyAndId>();
        Edge edge = preBoardEdges.get(stop);
        if (edge == null)
            return new ArrayList<AgencyAndId>();
        for (Edge e : edge.getToVertex().getOutgoing()) {
            if (e instanceof TransitBoardAlight && ((TransitBoardAlight) e).isBoarding()) {
                TransitBoardAlight board = (TransitBoardAlight) e;
                out.add(board.getPattern().getExemplar().getRoute().getId());
            }
            if (e instanceof FrequencyBoard) {
                FrequencyBoard board = (FrequencyBoard) e;
                out.add(board.getPattern().getTrip().getRoute().getId());
            }
        }
        return new ArrayList<AgencyAndId>(out);
    }

    public void setCenter(Coordinate coord) {
        this.center = coord;
    }

    @Override
    public Coordinate getCenter() {
        return center;
    }

    public void setOvernightBreak(int overnightBreak) {
        this.overnightBreak = overnightBreak;
    }

    @Override
    public int getOvernightBreak() {
        return overnightBreak;
    }

    @Override
    public Collection<Stop> getStopsForRoute(AgencyAndId route) {
        return stopsForRoute.get(route);
    }
}
