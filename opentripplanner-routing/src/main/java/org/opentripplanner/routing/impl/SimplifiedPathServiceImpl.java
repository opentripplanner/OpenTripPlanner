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

import static org.opentripplanner.routing.automata.Nonterminal.choice;
import static org.opentripplanner.routing.automata.Nonterminal.plus;
import static org.opentripplanner.routing.automata.Nonterminal.seq;
import static org.opentripplanner.routing.automata.Nonterminal.star;

import java.util.List;

import lombok.Setter;

import org.opentripplanner.routing.algorithm.strategies.ThreadedBidirectionalHeuristic;
import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.SimpleTransfer;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.edgetype.StreetTransitLink;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.routing.vertextype.OffboardVertex;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class SimplifiedPathServiceImpl implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(SimplifiedPathServiceImpl.class);

    @Autowired @Setter
    private GraphService graphService;
    
    @Autowired @Setter
    private SPTService sptService;

    @Setter 
    private double timeout = 0; // seconds
    
    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {

        if (options == null) {
            LOG.error("PathService was passed a null routing request.");
            return null;
        }

        if (options.rctx == null) {
            options.setRoutingContext(graphService.getGraph(options.getRouterId()));
            options.rctx.pathParsers = new PathParser[] { new Parser() };
        }

        options.setMaxTransfers(Integer.MAX_VALUE);
        options.setMaxWalkDistance(Double.MAX_VALUE);
        //options.setMaxWalkDistance(60000);
        options.setWalkBoardCost(60*20);
        options.setBikeBoardCost(60*20);
        options.setWalkReluctance(1.25);
        options.setTransferSlack(60*4);
        options.setBoardSlack(60*2);
        options.setAlightSlack(60*2);
        
        // always use the threaded heuristic
        ThreadedBidirectionalHeuristic heuristic = 
                new ThreadedBidirectionalHeuristic(options.rctx.graph);

        options.rctx.remainingWeightHeuristic = heuristic;
        long searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN SEARCH");
        ShortestPathTree spt = sptService.getShortestPathTree(options, timeout);
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        heuristic.abort();
        
        if (spt == null) { // timeout or other fail
            LOG.warn("SPT was null.");
            return null;
        }
        spt.getPaths().get(0).dump();
        return spt.getPaths();
        
        /*
        List<? extends State> states = spt.getStates(options.rctx.target);
        if (states == null) {
            LOG.warn("no states.");
            return null;
        }
        if (states.size() == 0) {
            LOG.warn("0-length state list.");
            return null;
        }
        State s = states.get(0);
        options = options.clone();
        options.setArriveBy( ! options.isArriveBy());
        options.dateTime = s.getTime();
        options.rctx = new RoutingContext(options, options.rctx.graph, 
                options.rctx.fromVertex, options.rctx.toVertex, false);
        
        // existing heuristic has been aborted
        heuristic = new ThreadedBidirectionalHeuristic(options.rctx.graph);
        options.rctx.remainingWeightHeuristic = heuristic;
        
        searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN REVERSE SEARCH");
        spt = sptService.getShortestPathTree(options, timeout);
        LOG.debug("END SEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        heuristic.abort();
        
        // We order the list of returned paths by the time of arrival or departure (not path duration)
        //Collections.sort(paths, new PathComparator(options.isArriveBy()));
        //return Arrays.asList(path);
        List<GraphPath> paths = spt.getPaths(options.rctx.target, false); 
        paths.get(0).dump();
        return paths;
        */
    }

    public static class Parser extends PathParser {

        private static final int STREET = 1;
        private static final int LINK = 2;
        private static final int STATION = 3;
        private static final int RIDE = 4;
        private static final int XFER = 5;

        private static final DFA DFA;

        static {
            Nonterminal nontransitLeg = plus(STREET);
            Nonterminal transitLeg = seq(plus(STATION), plus(RIDE), plus(STATION));
            Nonterminal transitItinerary = seq(
                    nontransitLeg, 
                    LINK, transitLeg, star(XFER, transitLeg), LINK, 
                    nontransitLeg);
            Nonterminal itinerary = choice(nontransitLeg, transitItinerary);
            DFA = itinerary.toDFA().minimize();
            System.out.println(DFA.toGraphViz());
            System.out.println(DFA.dumpTable());
        }

        @Override
        protected DFA getDFA() {
            return DFA;
        }

        @Override
        public int terminalFor(State state) {
            Vertex v = state.getVertex();
            Edge e = state.getBackEdge();
            if (e == null || e instanceof StreetEdge)
                return STREET;
            if (e instanceof StreetTransitLink)
                return LINK;
            if (e instanceof SimpleTransfer)
                return XFER;
            if (v instanceof OnboardVertex)
                return RIDE;
            if (v instanceof OffboardVertex)
                return STATION;
            if (v instanceof StreetVertex)
                return STREET;
            else {
                LOG.debug("failed to tokenize path. vertex {} edge {}", v, e);
                throw new RuntimeException("failed to tokenize path");
            }
        }

    }
    
}

