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

package org.opentripplanner.graph_builder.impl.transit_index;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Route;
import org.onebusaway.gtfs.model.ServiceCalendar;
import org.onebusaway.gtfs.model.ServiceCalendarDate;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.model.StopTime;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.services.GtfsRelationalDao;
import org.opentripplanner.common.IterableLibrary;
import org.opentripplanner.graph_builder.services.GraphBuilderWithGtfsDao;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.edgetype.Alight;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.Dwell;
import org.opentripplanner.routing.edgetype.Hop;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.edgetype.TripPattern;
import org.opentripplanner.routing.graph.AbstractEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.TransitIndexService;
import org.opentripplanner.routing.transit_index.RouteSegment;
import org.opentripplanner.routing.transit_index.RouteVariant;
import org.opentripplanner.routing.transit_index.TransitIndexServiceImpl;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.opentripplanner.routing.vertextype.TransitVertex;
import org.opentripplanner.util.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Process GTFS to build transit index for use in patching
 * 
 * @author novalis
 * 
 */
public class TransitIndexBuilder implements GraphBuilderWithGtfsDao {
    private static final Logger _log = LoggerFactory.getLogger(TransitIndexBuilder.class);

    private GtfsRelationalDao dao;

    private HashMap<AgencyAndId, RouteVariant> variantsByTrip = new HashMap<AgencyAndId, RouteVariant>();

    private HashMap<AgencyAndId, List<RouteVariant>> variantsByRoute = new HashMap<AgencyAndId, List<RouteVariant>>();

    private HashMap<String, List<RouteVariant>> variantsByAgency = new HashMap<String, List<RouteVariant>>();

    private HashMap<AgencyAndId, Edge> preAlightEdges = new HashMap<AgencyAndId, Edge>();

    private HashMap<AgencyAndId, Edge> preBoardEdges = new HashMap<AgencyAndId, Edge>();

    private HashMap<AgencyAndId, HashSet<String>> directionsByRoute = new HashMap<AgencyAndId, HashSet<String>>();

    List<TraverseMode> modes = new ArrayList<TraverseMode>();

    @Override
    public void setDao(GtfsRelationalDao dao) {
        this.dao = dao;
    }

    @Override
    public void buildGraph(Graph graph) {
        _log.debug("Building transit index");

        for (TransitVertex gv : IterableLibrary.filter(graph.getVertices(), TransitVertex.class)) {
            boolean start = false;
            boolean noStart = false;
            TripPattern pattern = null;
            Trip trip = null;
            for (Edge e : gv.getIncoming()) {
                if (!(e instanceof AbstractEdge)) {
                    continue;
                }
                if (e instanceof PatternHop || e instanceof Alight || e instanceof PatternDwell || e instanceof Dwell) {
                    noStart = true;
                }
                if (e instanceof PatternBoard) {
                    pattern = ((PatternBoard) e).getPattern();
                    trip = pattern.getExemplar();
                    start = true;
                }
                if (e instanceof Board) {
                    trip = ((Board) e).getTrip();
                    start = true;
                }
                if (e instanceof PreBoardEdge) {
                    TransitStop stop = (TransitStop) e.getFromVertex();
                    preBoardEdges.put(stop.getStopId(), e);
                    start = false;
                }
                if (e instanceof PreAlightEdge) {
                    TransitStop stop = (TransitStop) ((PreAlightEdge) e).getToVertex();
                    preAlightEdges.put(stop.getStopId(), e);
                    start = false;
                }
            }
            if (start && !noStart) {
                RouteVariant variant = variantsByTrip.get(trip.getId());
                if (variant == null) {
                    variant = addTripToVariant(trip);
                    if (pattern != null) {
                        for (Trip trip2 : pattern.getTrips()) {
                            addModeFromTrip(trip2);
                            variantsByTrip.put(trip2.getId(), variant);
                        }
                    }
                } else {
                    continue;
                }

                boolean setExemplar = !variant.isExemplarSet();

                Edge prevHop = null;
                while (gv != null) {
                    RouteSegment segment = new RouteSegment(gv.getStopId());
                    segment.hopIn = prevHop;
                    for (Edge e : gv.getIncoming()) {
                        if (e instanceof Board || e instanceof PatternBoard) {
                            segment.board = e;
                        }
                    }
                    Collection<Edge> outgoing = gv.getOutgoing();
                    gv = null;
                    for (Edge e : outgoing) {
                        if (e instanceof PatternHop || e instanceof Hop) {
                            segment.hopOut = e;
                            gv = (TransitVertex) e.getToVertex();
                        }
                        if (e instanceof PatternDwell || e instanceof Dwell) {
                            segment.dwell = e;
                            for (Edge e2 : e.getToVertex().getOutgoing()) {
                                if (e2 instanceof PatternHop || e2 instanceof Hop) {
                                    segment.hopOut = e2;
                                    gv = (TransitVertex) e2.getToVertex();
                                }
                                if (e2 instanceof PatternAlight || e2 instanceof Alight) {
                                    segment.alight = e2;
                                }
                            }
                        }
                        if (e instanceof PatternAlight || e instanceof Alight) {
                            segment.alight = e;
                        }
                    }
                    prevHop = segment.hopOut;
                    if (setExemplar) {
                        variant.addExemplarSegment(segment);
                    }
                    variant.addSegment(segment);
                }
            }
        }

        nameVariants(variantsByRoute);
        int totalVariants = 0;
        int totalTrips = 0;
        for (List<RouteVariant> variants : variantsByRoute.values()) {
            totalVariants += variants.size();
            for (RouteVariant variant : variants) {
                variant.cleanup();
                totalTrips += variant.getTrips().size();
            }
        }
        _log.debug("Built transit index: " + variantsByAgency.size() + " agencies, "
                + variantsByRoute.size() + " routes, " + totalTrips + " trips, " + totalVariants
                + " variants ");

        TransitIndexService service = new TransitIndexServiceImpl(variantsByAgency,
                variantsByRoute, variantsByTrip, preBoardEdges, preAlightEdges, directionsByRoute,
                modes);

        insertCalendarData(service);
        graph.putService(TransitIndexService.class, service);
    }

    private void insertCalendarData(TransitIndexService service) {
        Collection<ServiceCalendar> allCalendars = dao.getAllCalendars();
        service.addCalendars(allCalendars);
        Collection<ServiceCalendarDate> allDates = dao.getAllCalendarDates();
        service.addCalendarDates(allDates);
    }

    private void addModeFromTrip(Trip trip) {
        TraverseMode mode = GtfsLibrary.getTraverseMode(trip.getRoute());
        if (!modes.contains(mode)) {
            modes.add(mode);
        }
    }

    private void nameVariants(HashMap<AgencyAndId, List<RouteVariant>> variantsByRoute) {
        for (List<RouteVariant> variants : variantsByRoute.values()) {
            Route route = variants.get(0).getRoute();
            String routeName = GtfsLibrary.getRouteName(route);

            /*
             * simplest case: there's only one route variant, so we'll just give it the route's name
             */
            if (variants.size() == 1) {
                variants.get(0).setName(routeName);
                continue;
            }

            /* next, do routes have a unique start, end, or via? */
            HashMap<String, List<RouteVariant>> starts = new HashMap<String, List<RouteVariant>>();
            HashMap<String, List<RouteVariant>> ends = new HashMap<String, List<RouteVariant>>();
            HashMap<String, List<RouteVariant>> vias = new HashMap<String, List<RouteVariant>>();
            for (RouteVariant variant : variants) {
                List<Stop> stops = variant.getStops();
                MapUtils.addToMapList(starts, getName(stops.get(0)), variant);
                MapUtils.addToMapList(ends, getName(stops.get(stops.size() - 1)), variant);
                for (Stop stop : stops) {
                    MapUtils.addToMapList(vias, getName(stop), variant);
                }
            }

            // do simple naming for unique start/end/via
            for (RouteVariant variant : variants) {
                List<Stop> stops = variant.getStops();
                String firstStop = getName(stops.get(0));
                if (starts.get(firstStop).size() == 1) {
                    // this is the only route with this start
                    String name = routeName + " from " + firstStop;
                    variant.setName(name);
                } else {
                    String lastStop = getName(stops.get(stops.size() - 1));
                    if (ends.get(lastStop).size() == 1) {
                        String name = routeName + " to " + lastStop;
                        variant.setName(name);
                    } else {
                        for (Stop stop : stops) {
                            String viaStop = getName(stop);
                            if (vias.get(viaStop).size() == 1) {
                                String name = routeName + " via " + viaStop;
                                variant.setName(name);
                                break;
                            }
                        }
                    }
                }
            }

            /**
             * now we have the case where no route has a unique start, stop, or via. This can happen
             * if you have a single route which serves trips on an H-shaped alignment, where trips
             * can start at A or B and end at either C or D, visiting the same sets of stops along
             * the shared segments.
             * 
             * <pre>
             *                    A      B
             * 	                  |      |
             *                    |------|
             *                    |      |
             *                    |      |
             *                    C      D
             * </pre>
             * 
             * First, we try unique start + end, then start + via + end, and if that doesn't work,
             * we check for expresses, and finally we use a random trip's id.
             * 
             * It can happen if there is an express and a local version of a given line where the
             * local starts and ends at the same place as the express but makes a strict superset of
             * stops; the local version will get a "via", but the express will be doomed.
             * 
             * We can first check for the local/express situation by saying that if there are a
             * subset of routes with the same start/end, and there is exactly one that can't be
             * named with start/end/via, call it "express".
             * 
             * Consider the following three trips (A, B, C) along a route with four stops. A is the
             * local, and gets "via stop 3"; B is a limited, and C is (logically) an express:
             * 
             * A,B,C -- A,B -- A -- A, B, C
             * 
             * Here, neither B nor C is nameable. If either were removed, the other would be called
             * "express".
             * 
             * 
             * 
             */
            for (RouteVariant variant : variants) {
                if (variant.getName() != null)
                    continue;
                List<Stop> stops = variant.getStops();
                String firstStop = getName(stops.get(0));
                HashSet<RouteVariant> remainingVariants = new HashSet<RouteVariant>(
                        starts.get(firstStop));

                String lastStop = getName(stops.get(stops.size() - 1));
                // take the intersection
                remainingVariants.retainAll(ends.get(lastStop));
                if (remainingVariants.size() == 1) {
                    String name = routeName + " from " + firstStop + " to "
                            + lastStop;
                    variant.setName(name);
                    continue;
                }
                // this did not yield a unique name; try start / via / end for
                // each via
                for (Stop stop : stops) {
                    if (getName(stop).equals(firstStop) || getName(stop).equals(lastStop)) {
                        continue;
                    }
                    List<RouteVariant> via = vias.get(getName(stop));
                    boolean found = false;
                    boolean bad = false;
                    for (RouteVariant viaVariant : via) {
                        if (remainingVariants.contains(viaVariant)) {
                            if (found) {
                                bad = true;
                                break;
                            } else {
                                found = true;
                            }
                        }
                    }
                    if (found && !bad) {
                        String name = routeName + " from " + firstStop + " to "
                                + lastStop + " via " + getName(stop);
                        variant.setName(name);
                        break;
                    }
                }
                if (variant.getName() == null) {
                    // check for express
                    if (remainingVariants.size() == 2) {
                        // there are exactly two remaining variants sharing this start/end
                        // we know that this one must be a subset of the other, because it
                        // has no unique via. So, it is the express

                        String name = routeName + " from " + firstStop + " to "
                                + lastStop + " express";
                        variant.setName(name);
                    } else {
                        // the final fallback
                        variant.setName(routeName + " like " + variant.getTrips().get(0).getId());
                    }
                }
            }

        }

    }

    private String getName(Stop stop) {
        return stop.getName() + " (" + stop.getId() + ")";
    }

    private RouteVariant addTripToVariant(Trip trip) {
        // have we seen this trip before?
        RouteVariant variant = variantsByTrip.get(trip.getId());
        if (variant != null) {
            variant.addTrip(trip);
            return variant;
        }

        AgencyAndId routeId = trip.getRoute().getId();
        String directionId = trip.getDirectionId();
        HashSet<String> directions = directionsByRoute.get(routeId);
        if (directions == null) {
            directions = new HashSet<String>();
            directionsByRoute.put(routeId, directions);
        }
        directions.add(directionId);

        // build the list of stops for this trip
        List<StopTime> stopTimes = dao.getStopTimesForTrip(trip);
        ArrayList<Stop> stops = new ArrayList<Stop>();
        for (StopTime stopTime : stopTimes) {
            stops.add(stopTime.getStop());
        }

        Route route = trip.getRoute();
        // see if we have a variant for this route like this already
        List<RouteVariant> agencyVariants = variantsByAgency.get(route.getId().getAgencyId());
        if (agencyVariants == null) {
            agencyVariants = new ArrayList<RouteVariant>();
            variantsByAgency.put(route.getId().getAgencyId(), agencyVariants);
        }
        List<RouteVariant> variants = variantsByRoute.get(route.getId());
        if (variants == null) {
            variants = new ArrayList<RouteVariant>();
            variantsByRoute.put(route.getId(), variants);
        }
        for (RouteVariant existingVariant : variants) {
            if (existingVariant.getStops().equals(stops)) {
                variant = existingVariant;
                break;
            }
        }
        if (variant == null) {
            // create a variant for these stops on this route
            variant = new RouteVariant(route, stops);
            variants.add(variant);
            agencyVariants.add(variant);
        }
        variantsByTrip.put(trip.getId(), variant);
        variant.addTrip(trip);
        return variant;
    }
}
