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
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.algorithm.strategies.TableRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.WeightTable;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.GraphVertex;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;

@Component
public class ContractionPathServiceImpl implements PathService {

    private static final int MAX_TIME_FACTOR = 2;

    private static final int MAX_WEIGHT_FACTOR = 2;

    private static final Logger LOG = LoggerFactory.getLogger(ContractionPathServiceImpl.class);

    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    private static final Pattern _latLonPattern = Pattern.compile("^\\s*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\s*$");

    private GraphService _graphService;

    private RoutingService _routingService;

    private StreetVertexIndexService _indexService;

    public GraphService getGraphService() {
        return _graphService;
    }

    @Autowired
    public void setGraphService(GraphService graphService) {
        _graphService = graphService;
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

        Vertex origin = null;
        Vertex target = null;

        if (options.isArriveBy()) {
            origin = toVertex;
            target = fromVertex;
        } else {
            origin = fromVertex;
            target = toVertex;
        }

        State state = new State((int)(targetTime.getTime() / 1000), origin, options);

        return plan(state, target, nItineraries);
    }

    @Override
    public List<GraphPath> plan(State origin, Vertex target, int nItineraries) {

        Date targetTime = new Date(origin.getTime() * 1000);
        TraverseOptions options = origin.getOptions();

        if (_graphService.getCalendarService() != null)
            options.setCalendarService(_graphService.getCalendarService());
        options.setTransferTable(_graphService.getGraph().getTransferTable());
        options.setServiceDays(targetTime.getTime() / 1000);
        if (options.getModes().getTransit()
                && !_graphService.getGraph().transitFeedCovers(targetTime)) {
            // user wants a path through the transit network,
            // but the date provided is outside those covered by the transit feed.
            throw new TransitTimesException();
        }
        // decide which A* heuristic to use
        if (_graphService.getGraph().hasService(WeightTable.class)
                && options.getModes().getTransit()) {
            options.remainingWeightHeuristic = new TableRemainingWeightHeuristic(_graphService
                    .getGraph());
            LOG
                    .debug("Weight table present in graph and transit itinerary requested. Using table-driven A* heuristic.");
        } else {
            LOG
                    .debug("No weight table in graph or non-transit itinerary requested. Keeping existing A* heuristic.");
        }

        // EXPERIMENTAL
        // options.remainingWeightHeuristic = new
        // LBGRemainingWeightHeuristic(_graphService.getGraph());

        // If transit is not to be used, disable walk limit and only search for one itinerary.
        if (!options.getModes().getTransit()) {
            nItineraries = 1;
            options.setMaxWalkDistance(Double.MAX_VALUE);
        }

        ArrayList<GraphPath> paths = new ArrayList<GraphPath>();

        // The list of options specifying various modes, banned routes, etc to try for multiple
        // itineraries
        Queue<TraverseOptions> optionQueue = new LinkedList<TraverseOptions>();
        optionQueue.add(options);

        /* if the user wants to travel by transit, create a bus-only set of options */
        if (options.getModes().getTrainish() && options.getModes().contains(TraverseMode.BUS)) {
            TraverseOptions busOnly = options.clone();
            busOnly.setModes(options.getModes().clone());
            busOnly.getModes().setTrainish(false);
            // Moved inside block to avoid double insertion in list ? (AMB)
            // optionQueue.add(busOnly);
        }

        double maxWeight = Double.MAX_VALUE;
        long maxTime = options.isArriveBy() ? 0 : Long.MAX_VALUE;
        while (paths.size() < nItineraries) {
            options = optionQueue.poll();
            if (options == null) {
                break;
            }
            StateEditor editor = new StateEditor(origin, null);
            editor.setTraverseOptions(options);
            origin = editor.makeState();

            // options.worstTime = maxTime;
            // options.maxWeight = maxWeight;
            long searchBeginTime = System.currentTimeMillis();
            LOG.debug("BEGIN SEARCH");
            List<GraphPath> somePaths = _routingService.route(origin, target);
            LOG.debug("END SEARCH {} msec", System.currentTimeMillis() - searchBeginTime);
            if (maxWeight == Double.MAX_VALUE) {
                /*
                 * the worst trip we are willing to accept is at most twice as bad or twice as long.
                 */
                if (somePaths.isEmpty()) {
                    // if there is no first path, there won't be any other paths
                    return null;
                }
                GraphPath path = somePaths.get(0);
                long duration = path.getDuration();
                LOG.debug("Setting max time and weight for subsequent searches.");
                LOG.debug("First path start time:  {}", path.getStartTime());
                maxTime = path.getStartTime() + 
                		  MAX_TIME_FACTOR * (options.isArriveBy() ? -duration : duration);
                LOG.debug("First path duration:  {}", duration);
                LOG.debug("Max time set to:  {}", maxTime);
                maxWeight = path.getWeight() * MAX_WEIGHT_FACTOR;
                LOG.debug("Max weight set to:  {}", maxWeight);
            }
            if (somePaths.isEmpty()) {
                LOG.debug("NO PATHS FOUND");
                continue;
            }
            for (GraphPath path : somePaths) {
                if (!paths.contains(path)) {
                    // DEBUG
                    // path.dump();
                    paths.add(path);
                    // now, create a list of options, one with each trip in this journey banned.

                    LOG.debug("New trips: {}", path.getTrips());
                    TraverseOptions newOptions = options.clone();
                    for (AgencyAndId trip : path.getTrips()) {
                        newOptions.bannedTrips.add(trip);
                    }

                    if (!optionQueue.contains(newOptions)) {
                        optionQueue.add(newOptions);
                    }
                    /*
                     * // now, create a list of options, one with each route in this trip banned. //
                     * the HashSet banned is not strictly necessary as the optionsQueue will //
                     * already remove duplicate options, but it might be slightly faster as //
                     * hashing TraverseOptions is slow. LOG.debug("New routespecs: {}",
                     * path.getRouteSpecs()); for (RouteSpec spec : path.getRouteSpecs()) {
                     * TraverseOptions newOptions = options.clone();
                     * newOptions.bannedRoutes.add(spec); if (!optionQueue.contains(newOptions)) {
                     * optionQueue.add(newOptions); } }
                     */
                }
            }
            LOG.debug("{} / {} itineraries", paths.size(), nItineraries);
        }
        if (paths.size() == 0) {
            return null;
        }
        // We order the list of returned paths by the time of arrival or departure (not path duration)
        Collections.sort(paths, new PathComparator(origin.getOptions().isArriveBy()));
        return paths;
    }

    @Override
    public List<GraphPath> plan(String fromPlace, String toPlace, List<String> intermediates,
            Date targetTime, TraverseOptions options) {

        if (options.getModes().contains(TraverseMode.TRANSIT)) {
            throw new UnsupportedOperationException("TSP is not supported for transit trips");
        }

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

        if (_graphService.getCalendarService() != null)
            options.setCalendarService(_graphService.getCalendarService());

        options.setTransferTable(_graphService.getGraph().getTransferTable());
        GraphPath path = _routingService.route(fromVertex, toVertex, intermediateVertices,
                (int)(targetTime.getTime() / 1000), options);

        return Arrays.asList(path);
    }

    private Vertex getVertexForPlace(String place, TraverseOptions options) {

        Matcher matcher = _latLonPattern.matcher(place);

        if (matcher.matches()) {
            double lat = Double.parseDouble(matcher.group(1));
            double lon = Double.parseDouble(matcher.group(4));
            Coordinate location = new Coordinate(lon, lat);
            return _indexService.getClosestVertex(location, options);
        }

        return _graphService.getContractionHierarchySet().getVertex(place);
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
        Graph graph = _graphService.getGraph();
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
