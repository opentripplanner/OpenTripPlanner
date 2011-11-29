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
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.BidirectionalRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.ExtraEdgesStrategy;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.Edge;
import org.opentripplanner.routing.core.OverlayGraph;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.core.Vertex;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.pqueue.BinHeap;
import org.opentripplanner.routing.spt.GraphPath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
/**
 * Implements a multi-objective goal-directed search algorithm like the one in Sec. 4.2 of: 
 * Perny and Spanjaard. Near Admissible Algorithms for Multiobjective Search.
 * 
 * The ideas being tested here are:
 * Pruning search based on paths already found 
 * Near-admissible search / relaxed dominance 
 * Allow resource constraints on transfers and walking
 * 
 * This approach seems to need a very accurate heuristic to achieve reasonable run times, 
 * so for now it is hard-coded to use the Bidirectional heuristic. 
 * 
 * It will return a list of paths in order of increasing weight, starting at a weight very
 * close to that of the optimum path. These paths can vary quite a bit in terms of transfers, 
 * walk distance, and trips taken.
 * 
 * Because the number of boardings and walk distance are considered incomparable
 * to other weight components, including aggregate weight itself, paths can be 
 * pruned due to excessive walking distance or excessive number of transfers 
 * without compromising other paths. 
 * 
 * This path service cannot be used with edges that return multiple (chained) states.
 *
 * @author andrewbyrd
 */
@Component
public class MultiObjectivePathServiceImpl extends GenericPathService {

    private static final Logger LOG = LoggerFactory.getLogger(MultiObjectivePathServiceImpl.class);

    private double[] _timeouts = new double[] {4, 2, 0.6, 0.4}; // seconds
    
    private double _maxPaths = 4;

    private TraverseVisitor traverseVisitor;

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

    public void setTraverseVisitor(TraverseVisitor traverseVisitor) {
        this.traverseVisitor = traverseVisitor;
    }

    @Override
    public List<GraphPath> plan(NamedPlace fromPlace, NamedPlace toPlace, Date targetTime,
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

        TraverseOptions options = origin.getOptions();

        if (_graphService.getCalendarService() != null)
            options.setCalendarService(_graphService.getCalendarService());
        options.setTransferTable(_graphService.getGraph().getTransferTable());

        options.setServiceDays(origin.getTime());
        if (options.getModes().getTransit()
            && !_graphService.getGraph().transitFeedCovers(new Date(origin.getTime() * 1000))) {
            // user wants a path through the transit network,
            // but the date provided is outside those covered by the transit feed.
            throw new TransitTimesException();
        }
        
        // always use the bidirectional heuristic because the others are not precise enough
        RemainingWeightHeuristic heuristic = new BidirectionalRemainingWeightHeuristic(_graphService.getGraph());
                
        // the states that will eventually be turned into paths and returned
        List<State> returnStates = new LinkedList<State>();

        // Populate any extra edges
        final ExtraEdgesStrategy extraEdgesStrategy = options.extraEdgesStrategy;
        OverlayGraph extraEdges = new OverlayGraph();
        extraEdgesStrategy.addEdgesFor(extraEdges, origin.getVertex());
        extraEdgesStrategy.addEdgesFor(extraEdges, target);
        
        BinHeap<State> pq = new BinHeap<State>();
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

            State su = pq.extract_min();

            for (State bs : boundingStates) {
                if (eDominates(bs, su)) {
                    continue QUEUE;
                }
            }

            Vertex u = su.getVertex();

            if (traverseVisitor != null) {
                traverseVisitor.visitVertex(su);
            }

            if (u.equals(target)) {
                boundingStates.add(su);
                returnStates.add(su);
                if ( ! options.getModes().getTransit())
                    break QUEUE;
                // options should contain max itineraries
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
            
            EDGE: for (Edge e : u.getEdges(extraEdges, null, options.isArriveBy())) {
                State new_sv = e.traverse(su);
                if (traverseVisitor != null) {
                    traverseVisitor.visitEdge(e, new_sv);
                }

                if (new_sv == null)
                    continue;
                double h = heuristic.computeForwardWeight(new_sv, target);
                for (State bs : boundingStates) {
                    if (eDominates(bs, new_sv)) {
                        continue EDGE;
                    }
                }
                Vertex v = new_sv.getVertex();
                List<State> old_states = states.get(v);
                if (old_states == null) {
                    old_states = new LinkedList<State>();
                    states.put(v, old_states);
                } else {
                    for (State old_sv : old_states) {
                        if (eDominates(old_sv, new_sv)) {
                            continue EDGE;
                        }
                    }
                    Iterator<State> iter = old_states.iterator();
                    while (iter.hasNext()) {
                        State old_sv = iter.next();
                        if (eDominates(new_sv, old_sv)) {
                            iter.remove();
                        }
                    }
                }
                if (traverseVisitor != null)
                    traverseVisitor.visitEnqueue(new_sv);

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
        return s0.getWeight() <= s1.getWeight() * (1 + EPSILON) &&
               s0.getTime() <= s1.getTime() * (1 + EPSILON) &&
               s0.getWalkDistance() <= s1.getWalkDistance() * (1 + EPSILON) && 
               s0.getNumBoardings() <= s1.getNumBoardings();
    }

    @Override
    public List<GraphPath> plan(NamedPlace fromPlace, NamedPlace toPlace, List<NamedPlace> intermediates,
            boolean ordered, Date targetTime, TraverseOptions options) {
        return null;
    }

}
