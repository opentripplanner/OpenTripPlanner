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

package org.opentripplanner.routing.impl.raptor;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.ServiceDay;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseModeSet;
import org.opentripplanner.routing.edgetype.Board;
import org.opentripplanner.routing.edgetype.PatternAlight;
import org.opentripplanner.routing.edgetype.PatternBoard;
import org.opentripplanner.routing.edgetype.PatternDwell;
import org.opentripplanner.routing.edgetype.PatternHop;
import org.opentripplanner.routing.edgetype.PreAlightEdge;
import org.opentripplanner.routing.edgetype.PreBoardEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.pathparser.BasicPathParser;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.vertextype.TransitStop;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class Raptor implements PathService {
    private static final Logger log = LoggerFactory.getLogger(Raptor.class);

    static final double MAX_TRANSIT_SPEED = 25;

    @Autowired
    private GraphService graphService;

    private List<ServiceDay> cachedServiceDays;

    private RaptorData cachedRaptorData;

    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {

        if (options.rctx == null) {
            options.setRoutingContext(graphService.getGraph(options.getRouterId()));
            options.rctx.pathParsers = new PathParser[1];
            options.rctx.pathParsers[0] = new BasicPathParser();
        }
        Graph graph = graphService.getGraph(options.getRouterId());
        RaptorData data = graph.getService(RaptorDataService.class).getData();

        RoutingRequest walkOptions = options.clone();
        walkOptions.rctx.pathParsers = new PathParser[0];
        TraverseModeSet modes = options.getModes().clone();
        modes.setTransit(false);
        walkOptions.setModes(modes);
        RaptorSearch routeSet = new RaptorSearch(data, options);

        if (data.maxTransitRegions != null) {
            Calendar tripDate = Calendar.getInstance(graph.getTimeZone());
            tripDate.setTime(new Date(1000L * options.dateTime));

            Calendar maxTransitStart = Calendar.getInstance(graph.getTimeZone());
            maxTransitStart.set(Calendar.YEAR, data.maxTransitRegions.startYear);
            maxTransitStart.set(Calendar.MONTH, data.maxTransitRegions.startMonth);
            maxTransitStart.set(Calendar.DAY_OF_MONTH, data.maxTransitRegions.startDay);

            int day = 0;
            while (tripDate.after(maxTransitStart)) {
                day++;
                tripDate.add(Calendar.DAY_OF_MONTH, -1);
            }
            if (day > data.maxTransitRegions.maxTransit.length || options.isWheelchairAccessible()) {
                day = -1;
            }

            routeSet.maxTimeDayIndex = day;
        }

        for (int i = 0; i < options.getMaxTransfers() + 2; ++i) {
            round(data, options, walkOptions, routeSet, i);

            if (routeSet.getTargetStates().size() >= options.getNumItineraries())
                break;
        }

        if (routeSet.getTargetStates().isEmpty()) {
            log.info("RAPTOR found no paths (try retrying?)");
        }

        List<GraphPath> paths = new ArrayList<GraphPath>();
        for (RaptorState targetState : routeSet.getTargetStates()) {
            // reconstruct path
            ArrayList<RaptorState> states = new ArrayList<RaptorState>();
            RaptorState cur = targetState;
            while (cur != null) {
                states.add(cur);
                cur = cur.parent;
            }
            // states is in reverse order of time
            State state = getState(options, data, states);
            paths.add(new GraphPath(state, true));
        }

        return paths;
    }

    private State getState(RoutingRequest options, RaptorData data, ArrayList<RaptorState> states) {
        RaptorState cur;
        State state = new State(options);
        for (int i = states.size() - 1; i >= 0; --i) {
            cur = states.get(i);
            if (cur.walkPath != null) {
                GraphPath path = new GraphPath(cur.walkPath, true);
                for (Edge e : path.edges) {
                    State oldState = state;
                    state = e.traverse(state);
                    if (state == null) {
                        e.traverse(oldState);
                    }
                }
            } else {
                // so, cur is at this point at a transit stop; we have a route to board
                for (Edge e : state.getVertex().getOutgoing()) {
                    if (e instanceof PreBoardEdge) {
                        state = e.traverse(state);
                    }
                }
                PatternBoard board = cur.route.boards[cur.boardStopSequence][cur.patternIndex];
                State oldState = state;
                state = board.traverse(state);
                if (state == null) {
                    state = board.traverse(oldState);
                }
                // now traverse the hops and dwells until we find the alight we're looking for
                HOP: while (true) {
                    for (Edge e : state.getVertex().getOutgoing()) {
                        if (e instanceof PatternDwell) {
                            state = e.traverse(state);
                        } else if (e instanceof PatternHop) {
                            state = e.traverse(state);
                            for (Edge e2 : state.getVertex().getOutgoing()) {
                                if (e2 instanceof PatternAlight) {
                                    for (Edge e3 : e2.getToVertex().getOutgoing()) {
                                        if (e3 instanceof PreAlightEdge) {
                                            if (data.raptorStopsForStopId.get(((TransitStop) e3
                                                    .getToVertex()).getStopId()) == cur.stop) {
                                                state = e2.traverse(state);
                                                state = e3.traverse(state);
                                                break HOP;
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return state;
    }

    /**
     * Prune raptor data to include only routes and boardings which have trips today. Doesn't
     * actually improve speed
     */
    @SuppressWarnings("unchecked")
    private RaptorData pruneDataForServiceDays(Graph graph, ArrayList<ServiceDay> serviceDays) {

        if (serviceDays.equals(cachedServiceDays))
            return cachedRaptorData;
        // you are here: need to reduce list of boards
        RaptorData data = graph.getService(RaptorDataService.class).getData();
        RaptorData pruned = new RaptorData();
        pruned.raptorStopsForStopId = data.raptorStopsForStopId;
        pruned.stops = data.stops;
        pruned.routes = new ArrayList<RaptorRoute>();
        pruned.routesForStop = new List[pruned.stops.length];

        for (RaptorRoute route : data.routes) {
            ArrayList<Integer> keep = new ArrayList<Integer>();

            for (int i = 0; i < route.boards[0].length; ++i) {
                Edge board = route.boards[0][i];
                int serviceId;
                if (board instanceof PatternBoard) {
                    serviceId = ((PatternBoard) board).getPattern().getServiceId();
                } else if (board instanceof Board) {
                    serviceId = ((Board) board).getServiceId();
                } else {
                    log.debug("Unexpected nonboard among boards");
                    continue;
                }
                for (ServiceDay day : serviceDays) {
                    if (day.serviceIdRunning(serviceId)) {
                        keep.add(i);
                        break;
                    }
                }
            }
            if (keep.isEmpty())
                continue;
            int nPatterns = keep.size();
            RaptorRoute prunedRoute = new RaptorRoute(route.getNStops(), nPatterns);
            for (int stop = 0; stop < route.getNStops() - 1; ++stop) {
                for (int pattern = 0; pattern < nPatterns; ++pattern) {
                    prunedRoute.boards[stop][pattern] = route.boards[stop][keep.get(pattern)];
                }
            }
            pruned.routes.add(route);
            for (RaptorStop stop : route.stops) {
                List<RaptorRoute> routes = pruned.routesForStop[stop.index];
                if (routes == null) {
                    routes = new ArrayList<RaptorRoute>();
                    pruned.routesForStop[stop.index] = routes;
                }
                routes.add(route);
            }
        }
        for (RaptorStop stop : data.stops) {
            if (pruned.routesForStop[stop.index] == null) {
                pruned.routesForStop[stop.index] = Collections.emptyList();
            }
        }
        cachedServiceDays = serviceDays;
        cachedRaptorData = pruned;
        return pruned;
    }

    private void round(RaptorData data, RoutingRequest options, RoutingRequest walkOptions,
            final RaptorSearch search, int nBoardings) {

        log.debug("Round " + nBoardings);

        /* Phase 2: handle transit */
        List<RaptorState> createdStates = search.transitPhase(options, nBoardings);

        /* Phase 3: handle walking paths */

        search.walkPhase(options, walkOptions, nBoardings,
                createdStates);
    }

}
