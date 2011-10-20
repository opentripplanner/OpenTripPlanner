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
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.routing.algorithm.GraphLibrary;
import org.opentripplanner.routing.algorithm.strategies.BidirectionalRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.DirectEdge;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TransitStop;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.edgetype.OutEdge;
import org.opentripplanner.routing.edgetype.PlainStreetEdge;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.TurnEdge;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.services.StreetVertexIndexService;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vividsolutions.jts.geom.Coordinate;

@Component
public class MultiObjectivePathServiceImpl implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(MultiObjectivePathServiceImpl.class);

    private static final String _doublePattern = "-{0,1}\\d+(\\.\\d+){0,1}";

    private static final Pattern _latLonPattern = Pattern.compile("^\\s*(" + _doublePattern
            + ")(\\s*,\\s*|\\s+)(" + _doublePattern + ")\\s*$");

    private GraphService _graphService;

    private StreetVertexIndexService _indexService;
    
    private RemainingWeightHeuristicFactory _remainingWeightHeuristicFactory;
    
    private double[] _timeouts = new double[] {4, 2, 0.6, 0.4}; // seconds
    
    private double _maxPaths = 4;

    /**
     * Give up on searching for itineraries after this many seconds have elapsed.
     */
    public void setTimeouts (List<Double> timeouts) {
        _timeouts = new double[timeouts.size()];
        int i = 0;
        for (Double d : timeouts)
            _timeouts[i++] = d;
    }

    public void setMaxPaths(double numPaths) {
        _maxPaths = numPaths;
    }

    @Autowired
    public void setRemainingWeightHeuristicFactory(RemainingWeightHeuristicFactory hf) {
        _remainingWeightHeuristicFactory = hf;
    }

    @Autowired
    public void setGraphService(GraphService graphService) {
        _graphService = graphService;
    }

    public GraphService getGraphService() {
        return _graphService;
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
        Graph graph = _graphService.getGraph();
        options.remainingWeightHeuristic = new BidirectionalRemainingWeightHeuristic(graph);
        RemainingWeightHeuristic heuristic = options.remainingWeightHeuristic;
        
//                _remainingWeightHeuristicFactory.getInstanceForSearch(options, target);
//        LOG.debug("Applied A* heuristic: {}", options.remainingWeightHeuristic);

                
        // the states that will eventually be turned into graphpaths and returned
        List<State> returnStates = new LinkedList<State>();
//        options.setMaxWalkDistance(Double.MAX_VALUE);

        // Populate any extra edges
        final ExtraEdgesStrategy extraEdgesStrategy = options.extraEdgesStrategy;
        Map<Vertex, List<Edge>> extraEdges = new HashMap<Vertex, List<Edge>>();
        if (options.isArriveBy()) {
            extraEdgesStrategy.addIncomingEdgesForOrigin(extraEdges, origin.getVertex());
            extraEdgesStrategy.addIncomingEdgesForTarget(extraEdges, target);
        } else {
            extraEdgesStrategy.addOutgoingEdgesForOrigin(extraEdges, origin.getVertex());
            extraEdgesStrategy.addOutgoingEdgesForTarget(extraEdges, target);
        }
        if (extraEdges.isEmpty())
            extraEdges = Collections.emptyMap();
        
        BinHeap<State> pq = new BinHeap<State>();
        HashSet<Vertex> closed = new HashSet<Vertex>();
        List<State> boundingStates = new ArrayList<State>();
        
        HashMap<Vertex, List<State>> states = new HashMap<Vertex, List<State>>();
        pq.reset();
        pq.insert(origin, 0);
        heuristic.computeInitialWeight(origin, target);
        long startTime = System.currentTimeMillis();
        long endTime = startTime + (int)(_timeouts[0] * 1000);
        QUEUE: while ( ! pq.empty()) {
            if (System.currentTimeMillis() > endTime) {
                LOG.debug("timeout at {} msec", System.currentTimeMillis() - startTime);
                break QUEUE;
            }
            Double su_hweight = pq.peek_min_key();
            State su = pq.extract_min();
//                if (returnStates.size() > 0)
//                    if (su.getWalkDistance() > maxWalk)
//                        continue;

            for (State bs : boundingStates) {
                if (eDominates(bs, su))
                    continue QUEUE;
//                    if (su_hweight > bs.getWeight() * 1.5)
//                        break QUEUE;
            }
            Vertex u = su.getVertex();
            // check for dominated states 
            // (is this important? seems to make insignificant diff. are there the "hidden states"?)
//                List<State> u_states = states.get(u);
//                if (u_states != null && ! u_states.contains(su))
//                    continue;
            if (u.equals(target)) {
                boundingStates.add(su);
                returnStates.add(su);
                // options should contain max itineraries
                if ( ! options.getModes().getTransit())
                    break QUEUE;
                if (returnStates.size() >= _maxPaths)
                    break QUEUE;
                if (returnStates.size() < _timeouts.length) {
                    endTime = startTime + (int)(_timeouts[returnStates.size()] * 1000);
                    LOG.debug("{} path, set timeout to {}", 
                              returnStates.size(), 
                              _timeouts[returnStates.size()] * 1000);
                }
                continue QUEUE;
            }
            
            Collection<Edge> edges;
            if (options.isArriveBy())
                edges = GraphLibrary.getIncomingEdges(graph, u, extraEdges);
            else
                edges = GraphLibrary.getOutgoingEdges(graph, u, extraEdges);

            EDGE: for (Edge e : edges) {
                State new_sv = e.traverse(su);
                if (new_sv == null)
                    continue;
                double h = heuristic.computeForwardWeight(new_sv, target);
                for (State bs : boundingStates) {
                    if (eDominates(bs, new_sv))
                        continue;
                }
                Vertex v = new_sv.getVertex();
                List<State> old_states = states.get(v);
                if (old_states == null) {
                    old_states = new LinkedList<State>();
                    states.put(v, old_states);
                } else {
                    for (State old_sv : old_states) {
                        if (eDominates(old_sv, new_sv))
                            continue EDGE;
                    }
                    Iterator<State> iter = old_states.iterator();
                    while (iter.hasNext()) {
                        State old_sv = iter.next();
                        if (eDominates(new_sv, old_sv)) {
                            iter.remove();
                        }
                    }
                }
                old_states.add(new_sv);
                pq.insert(new_sv, new_sv.getWeight() + h);    
            }
        }
        
        // Make the states into paths and return them
        List<GraphPath> paths = new LinkedList<GraphPath>();
        for (State s : returnStates)
            paths.add(new GraphPath(s, true));
        return paths;
    }

    private boolean eDominates(State s0, State s1) {
        final double EPSILON = 0.05;
     // intended to avoid alternate options that use the same trips, but compromises results
//        if (s0.similarTripSeq(s1)) { 
//            return s0.getWeight() <= s1.getWeight() * (1 + EPSILON);
//        }
        return s0.getWeight() <= s1.getWeight() * (1 + EPSILON) &&
               s0.getTime() <= s1.getTime() * (1 + EPSILON) &&
               s0.getWalkDistance() <= s1.getWalkDistance() * (1 + EPSILON) && 
               s0.getNumBoardings() <= s1.getNumBoardings();
    }

    @Override
    public List<GraphPath> plan(String fromPlace, String toPlace, List<String> intermediates,
            Date targetTime, TraverseOptions options) {
        return null;
    }
    
    
    
    /* MOVE THESE METHODS TO A LIBRARY CLASS */
    
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

    public List<DirectEdge> getOutgoingEdges(Vertex vertex) {
        List<DirectEdge> result = new ArrayList<DirectEdge>();
        for (Edge out : vertex.getOutgoing()) {

            if (!(out instanceof TurnEdge || out instanceof OutEdge || out instanceof PlainStreetEdge)) {
                continue;
            }
            result.add((StreetEdge) out);
        }
        return result;
	}
}
