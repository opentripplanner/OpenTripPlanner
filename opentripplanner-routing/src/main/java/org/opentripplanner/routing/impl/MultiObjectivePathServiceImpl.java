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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.opentripplanner.routing.algorithm.TraverseVisitor;
import org.opentripplanner.routing.algorithm.strategies.BidirectionalRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.RemainingWeightHeuristic;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.common.pqueue.BinHeap;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.util.monitoring.MonitoringStore;
import org.opentripplanner.util.monitoring.MonitoringStoreFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
//@Component
public class MultiObjectivePathServiceImpl implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(MultiObjectivePathServiceImpl.class);

    private static final MonitoringStore store = MonitoringStoreFactory.getStore();
    
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
    public List<GraphPath> getPaths(RoutingRequest options) {

        // always use the bidirectional heuristic because the others are not precise enough
        RemainingWeightHeuristic heuristic = new BidirectionalRemainingWeightHeuristic(options.rctx.graph);
        // TODO: some way to ensure that this is set to bidi heuristic
        //options.rctx.remainingWeightHeuristic = heuristic;
        
        // the states that will eventually be turned into paths and returned
        List<State> returnStates = new LinkedList<State>();

        BinHeap<State> pq = new BinHeap<State>();
//        List<State> boundingStates = new ArrayList<State>();
        
        Vertex originVertex = options.rctx.origin;
        Vertex targetVertex = options.rctx.target;
        
        // increase maxWalk repeatedly in case hard limiting is in use 
        WALK: for (double maxWalk = options.getMaxWalkDistance();
                          maxWalk < 100000 && returnStates.isEmpty();
                          maxWalk *= 2) {
            LOG.debug("try search with max walk {}", maxWalk);
            // increase maxWalk if settings make trip impossible
            if (maxWalk < Math.min(originVertex.distance(targetVertex), 
                originVertex.getDistanceToNearestTransitStop() +
                targetVertex.getDistanceToNearestTransitStop())) 
                continue WALK;
            options.setMaxWalkDistance(maxWalk);
            
            // cap search / heuristic weight
            final double AVG_TRANSIT_SPEED = 25; // m/sec 
            double cutoff = (originVertex.distance(targetVertex) * 1.5) / AVG_TRANSIT_SPEED; // wait time is irrelevant in the heuristic
            cutoff += options.getMaxWalkDistance() * options.walkReluctance;
            options.maxWeight = cutoff;
            
            State origin = new State(options);
            // (used to) initialize heuristic outside loop so table can be reused
            heuristic.computeInitialWeight(origin, targetVertex);
            
            options.maxWeight = cutoff + 30 * 60 * options.waitReluctance;
            
            // reinitialize states for each retry
            HashMap<Vertex, List<State>> states = new HashMap<Vertex, List<State>>();
            pq.reset();
            pq.insert(origin, 0);
            long startTime = System.currentTimeMillis();
            long endTime = startTime + (int)(_timeouts[0] * 1000);
            LOG.debug("starttime {} endtime {}", startTime, endTime); 
            QUEUE: while ( ! pq.empty()) {
                
                if (System.currentTimeMillis() > endTime) {
                    LOG.debug("timeout at {} msec", System.currentTimeMillis() - startTime);
                    if (returnStates.isEmpty())
                        continue WALK;
                    else {
                        storeMemory();
                        break WALK;
                    }
                }
    
//                if (pq.peek_min_key() > options.maxWeight) {
//                    LOG.debug("max weight {} exceeded", options.maxWeight);
//                    break QUEUE;
//                }
                
                State su = pq.extract_min();
    
//                for (State bs : boundingStates) {
//                    if (eDominates(bs, su)) {
//                        continue QUEUE;
//                    }
//                }
    
                Vertex u = su.getVertex();
    
                if (traverseVisitor != null) {
                    traverseVisitor.visitVertex(su);
                }
    
                if (u.equals(targetVertex)) {
//                    boundingStates.add(su);
                    returnStates.add(su);
                    if ( ! options.getModes().isTransit())
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
                
                for (Edge e : options.isArriveBy() ? u.getIncoming() : u.getOutgoing()) {
                    STATE: for (State new_sv = e.traverse(su); new_sv != null; new_sv = new_sv.getNextResult()) {
                        if (traverseVisitor != null) {
                            traverseVisitor.visitEdge(e, new_sv);
                        }

                        double h = heuristic.computeForwardWeight(new_sv, targetVertex);
//                    for (State bs : boundingStates) {
//                        if (eDominates(bs, new_sv)) {
//                            continue STATE;
//                        }
//                    }
                        Vertex v = new_sv.getVertex();
                        List<State> old_states = states.get(v);
                        if (old_states == null) {
                            old_states = new LinkedList<State>();
                            states.put(v, old_states);
                        } else {
                            for (State old_sv : old_states) {
                                if (eDominates(old_sv, new_sv)) {
                                    continue STATE;
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
            }
        }
        storeMemory();

        // Make the states into paths and return them
        List<GraphPath> paths = new LinkedList<GraphPath>();
        for (State s : returnStates) {
            LOG.debug(s.toStringVerbose());
            paths.add(new GraphPath(s, true));
        }
        // sort by arrival time, though paths are already in order of increasing difficulty
        // Collections.sort(paths, new PathComparator(origin.getOptions().isArriveBy()));
        return paths;
    }

    private void storeMemory() {
        if (store.isMonitoring("memoryUsed")) {
            System.gc();
            long memoryUsed = Runtime.getRuntime().totalMemory() -
                    Runtime.getRuntime().freeMemory();
            store.setLongMax("memoryUsed", memoryUsed);
        }
    }

//    private boolean eDominates(State s0, State s1) {
//        final double EPSILON = 0.05;
//        return s0.getWeight() <= s1.getWeight() * (1 + EPSILON) &&
//               s0.getTime() <= s1.getTime() * (1 + EPSILON) &&
//               s0.getWalkDistance() <= s1.getWalkDistance() * (1 + EPSILON) && 
//               s0.getNumBoardings() <= s1.getNumBoardings();
//    }
    
    // TODO: move into an epsilon-dominance shortest path tree
    private boolean eDominates(State s0, State s1) {
        final double EPSILON = 0.05;
        if (s0.similarTripSeq(s1)) {
            return s0.getWeight() <= s1.getWeight() * (1 + EPSILON) &&
                    s0.getTime() <= s1.getTime() * (1 + EPSILON) &&
                    s0.getWalkDistance() <= s1.getWalkDistance() * (1 + EPSILON) && 
                    s0.getNumBoardings() <= s1.getNumBoardings();
        } else {
            return false;
        }
    }

//private boolean eDominates(State s0, State s1) {
//  final double EPSILON1 = 0.1;
//  if (s0.similarTripSeq(s1)) {
//      return  s0.getWeight()       <= s1.getWeight()       * (1 + EPSILON1) &&
//              s0.getElapsedTime()  <= s1.getElapsedTime()  * (1 + EPSILON1) &&
//              s0.getWalkDistance() <= s1.getWalkDistance() * (1 + EPSILON1) && 
//              s0.getNumBoardings() <= s1.getNumBoardings();
//  } else if (s0.getTripId() != null && s0.getTripId() == s1.getTripId()) {
//      return  s0.getNumBoardings() <= s1.getNumBoardings() &&
//    		  s0.getWeight()       <= s1.getWeight()       * (1 + EPSILON2) &&
//              s0.getElapsedTime()  <= s1.getElapsedTime()  * (1 + EPSILON2) &&
//              s0.getWalkDistance() <= s1.getWalkDistance() * (1 + EPSILON2);
//  } else {
//	  return false;
//  }
//}

    //    private boolean eDominates(State s0, State s1) {
//        if (s0.similarTripSeq(s1)) {
//            return s0.getWeight() <= s1.getWeight();
//        } else if (s0.getTrip() == s1.getTrip()) {
//            if (s0.getNumBoardings() < s1.getNumBoardings())
//                return true;
//            return s0.getWeight() <= s1.getWeight();
//        } else {
//            return false;
//        }
//    }

//    private boolean eDominates(State s0, State s1) {
//        final double EPSILON = 0.1;
//        return s0.getWeight() <= s1.getWeight() * (1 + EPSILON) &&
//                s0.getTime() <= s1.getTime() * (1 + EPSILON) && 
//               s0.getNumBoardings() <= s1.getNumBoardings();
//    }

}
