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

package org.opentripplanner.routing.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.gtfs.impl.calendar.CalendarServiceImpl;
import org.onebusaway.gtfs.model.Trip;
import org.onebusaway.gtfs.model.calendar.CalendarServiceData;
import org.opentripplanner.gtfs.GtfsLibrary;
import org.opentripplanner.routing.contraction.ContractionHierarchySet;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.RouteSpec;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.SPTEdge;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;

@Component
public class ContractionPathServiceImpl implements PathService {

    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    private static final Pattern _latLonPattern = Pattern.compile("^\\s*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\s*$");

    private ContractionHierarchySet hierarchies;

    private RoutingService _routingService;

    private StreetVertexIndexService _indexService;

    private CalendarServiceImpl _calendarService = null;

    @Autowired
    public void setHierarchies(ContractionHierarchySet hierarchies) {
        this.hierarchies = hierarchies;

        if (hierarchies.hasService(CalendarServiceData.class)) {
            CalendarServiceData data = hierarchies.getService(CalendarServiceData.class);
            CalendarServiceImpl calendarService = new CalendarServiceImpl();
            calendarService.setData(data);
            _calendarService = calendarService;
        }
    }

    @Autowired
    public void setRoutingService(RoutingService routingService) {
        _routingService = routingService;
    }

    @Autowired
    public void setIndexService(StreetVertexIndexService indexService) {
        _indexService = indexService;
    }

    @Override
    public List<GraphPath> plan(String fromPlace, String toPlace, Date targetTime,
            TraverseOptions options, int nItineraries) {

        ArrayList<String> notFound = new ArrayList<String>();
        Vertex fromVertex = getVertexForPlace(fromPlace, options);
        if (fromVertex == null) {
            notFound.add("from");
        }
        Vertex toVertex = getVertexForPlace(toPlace, options);
        if (toVertex == null) {
            notFound.add("to");
        }

        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }

        State state = new State(targetTime.getTime());

        if (_calendarService != null)
            options.setCalendarService(_calendarService);

        HashSet<GraphPath> paths = new HashSet<GraphPath>();

        Queue<TraverseOptions> optionQueue = new LinkedList<TraverseOptions>();
        optionQueue.add(options);
        /* if the user wants to travel by transit, create a bus-only set of options */
        if (options.modes.getTrainish() && options.modes.contains(TraverseMode.BUS)) {
            TraverseOptions busOnly = options.clone();
            busOnly.modes = options.modes.clone();
            busOnly.modes.setTrainish(false);
        }
        optionQueue.add(options);
        
        double maxWeight = Double.MAX_VALUE;
        long maxTime = options.getArriveBy() ? 0 : Long.MAX_VALUE;
        while (paths.size() < nItineraries) {
            options = optionQueue.poll();
            if (options == null) {
                break;
            }
            options.worstTime = maxTime;
            options.maxWeight = maxWeight;
            GraphPath path = _routingService.route(fromVertex, toVertex, state, options);
            if (maxWeight == Double.MAX_VALUE) {
                /* the worst trip we are willing to accept is at most twice
                 * as bad or twice as long.
                 */
                if (path == null) {
                    //if there is no first path, there won't be any other paths
                    return null;
                }
                maxWeight = path.vertices.lastElement().weightSum * 2;
                long tripTime = path.vertices.lastElement().state.getTime() - 
                    path.vertices.firstElement().state.getTime();
                if (options.getArriveBy()) {
                    maxTime = path.vertices.lastElement().state.getTime() - tripTime * 2;
                } else {
                    maxTime = path.vertices.firstElement().state.getTime() + tripTime * 2;
                }
            }
            if (path == null) {
                continue;
            }
            if (! paths.contains(path)) {
                paths.add(path);
                /* now, try various versions with blacklisted routes */
                for (SPTEdge spte : path.edges) {
                    Edge e = spte.payload;
                    if (e instanceof PatternBoard) {
                        Trip trip = spte.getTrip();
                        String routeName = GtfsLibrary.getRouteName(trip.getRoute());
                        RouteSpec spec = new RouteSpec(trip.getId().getAgencyId(), routeName);
                        TraverseOptions newOptions = options.clone();
                        newOptions.bannedRoutes.add(spec);
                        if (!optionQueue.contains(newOptions)) {
                            optionQueue.add(newOptions);
                        }
                    }
                }
            }
        }
        if (paths.size() == 0) {
            return null;
        }
        return new ArrayList<GraphPath>(paths);
    }

    @Override
    public List<GraphPath> plan(String fromPlace, String toPlace, List<String> intermediates,
            Date targetTime, TraverseOptions options) {

        ArrayList<String> notFound = new ArrayList<String>();
        Vertex fromVertex = getVertexForPlace(fromPlace, options);
        if (fromVertex == null) {
            notFound.add("from");
        }
        Vertex toVertex = getVertexForPlace(toPlace, options);
        if (toVertex == null) {
            notFound.add("to");
        }
        ArrayList<Vertex> intermediateVertices = new ArrayList<Vertex>();

        int i = 0;
        for (String intermediate : intermediates) {
            Vertex vertex = getVertexForPlace(intermediate, options);
            if (vertex == null) {
                notFound.add("intermediate." + i);
            } else {
                intermediateVertices.add(vertex);
            }
            i += 1;
        }

        if (notFound.size() > 0) {
            throw new VertexNotFoundException(notFound);
        }

        State state = new State(targetTime.getTime());

        if (_calendarService != null)
            options.setCalendarService(_calendarService);

        GraphPath path = _routingService.route(fromVertex, toVertex, intermediateVertices, state,
                options);

        return Arrays.asList(path);
    }

    private Vertex getVertexForPlace(String place, TraverseOptions options) {

        Matcher matcher = _latLonPattern.matcher(place);

        if (matcher.matches()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(4));
            Coordinate location = new Coordinate(lon, lat);
            return _indexService.getClosestVertex(hierarchies.getGraph(), location, options, true);
        }

        return hierarchies.getVertex(place);
    }

    @Override
    public boolean isAccessible(String place, TraverseOptions options) {
        /* fixme: take into account slope for wheelchair accessibility */
        Vertex vertex = getVertexForPlace(place, options);
        if (vertex instanceof TransitStop) {
            TransitStop ts = (TransitStop) vertex;
            return ts.hasWheelchairEntrance();
        } else if (vertex instanceof StreetLocation) {
            StreetLocation sl = (StreetLocation) vertex;
            return sl.isWheelchairAccessible();
        }
        return true;
    }

    public boolean multipleOptionsBefore(Edge edge) {
        Graph graph = hierarchies.getGraph();
        boolean foundAlternatePaths = false;
        Vertex start = edge.getFromVertex();
        GraphVertex gv = graph.getGraphVertex(start);
        if (gv == null) {
            return false;
        }
        for (Edge out : gv.getOutgoing()) {
            if (out == edge) {
                continue;
            }
            if (!(out instanceof TurnEdge || out instanceof OutEdge)) {
                continue;
            }
            // there were paths we didn't take.
            foundAlternatePaths = true;
            break;
        }
        return foundAlternatePaths;
    }

}
