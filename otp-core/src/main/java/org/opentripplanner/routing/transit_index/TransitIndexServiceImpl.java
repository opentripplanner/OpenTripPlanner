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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.onebusaway.gtfs.model.Agency;
import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.Stop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.FrequencyAlight;
import org.opentripplanner.routing.edgetype.FrequencyBasedTripPattern;
import org.opentripplanner.routing.edgetype.FrequencyBoard;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TableTripPattern;
import org.opentripplanner.routing.edgetype.TransitBoardAlight;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.util.MapUtils;

import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.vividsolutions.jts.geom.Coordinate;

public class TransitIndexServiceImpl implements TransitIndexService, Serializable {
    private static final long serialVersionUID = 20131127L;

    private HashMap<AgencyAndId, List<Stop>> stopsByStation;

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

    public TransitIndexServiceImpl(HashMap<AgencyAndId, List<Stop>> stopsByStation,
            HashMap<String, List<RouteVariant>> variantsByAgency,
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
        this.stopsByStation = stopsByStation;
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

    public void merge(HashMap<AgencyAndId, List<Stop>> stopsByStation,
            HashMap<String, List<RouteVariant>> variantsByAgency,
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
        MapUtils.mergeInUnique(this.stopsByStation, stopsByStation);
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
    public List<Stop> getStopsForStation(AgencyAndId stop) {
        List<Stop> list = stopsByStation.get(stop);
        if (list == null) {
            return Collections.emptyList();
        }

        return list;
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
        HashSet<AgencyAndId> routes = new HashSet<AgencyAndId>();

        for (TripPattern pattern : getTripPatternsForStop(stop)) {
            if (pattern instanceof TableTripPattern) {
                routes.add(((TableTripPattern) pattern).getExemplar().getRoute().getId());
            } else if (pattern instanceof FrequencyBasedTripPattern) {
                routes.add(((FrequencyBasedTripPattern) pattern).getTrip().getRoute().getId());
            }
        }

        return new ArrayList<AgencyAndId>(routes);
    }

    @Override
    public List<TripPattern> getTripPatternsForStop(AgencyAndId stop) {
        Edge alight = preAlightEdges.get(stop);
        Edge board = preBoardEdges.get(stop);
        HashSet<TripPattern> patterns = new HashSet<TripPattern>();

        if (alight != null) for (Edge edge : alight.getFromVertex().getIncoming()) {
            if (edge instanceof TransitBoardAlight && !(((TransitBoardAlight) edge).isBoarding())) {
                patterns.add(((TransitBoardAlight) edge).getPattern());
            } else if (edge instanceof FrequencyAlight) {
                patterns.add(((FrequencyAlight) edge).getPattern());
            }
        }

        if (board != null) for (Edge edge : board.getToVertex().getOutgoing()) {
            if (edge instanceof TransitBoardAlight && (((TransitBoardAlight) edge).isBoarding())) {
                patterns.add(((TransitBoardAlight) edge).getPattern());
            } else if (edge instanceof FrequencyBoard) {
                patterns.add(((FrequencyBoard) edge).getPattern());
            }
        }

        return new ArrayList<TripPattern>(patterns);
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
