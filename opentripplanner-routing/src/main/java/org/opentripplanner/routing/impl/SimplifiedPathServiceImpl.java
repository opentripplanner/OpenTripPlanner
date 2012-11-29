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

import java.util.Collections;
import java.util.List;

import lombok.Setter;

import org.opentripplanner.routing.algorithm.strategies.DefaultRemainingWeightHeuristic;
import org.opentripplanner.routing.algorithm.strategies.ThreadedBidirectionalHeuristic;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.google.common.collect.Lists;

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

        List<GraphPath> paths = Lists.newArrayList();

        if (options == null) {
            LOG.error("PathService was passed a null routing request.");
            return null;
        }

        if (options.rctx == null) {
            options.setRoutingContext(graphService.getGraph(options.getRouterId()));
            options.rctx.pathParsers = new PathParser[] {};
        }

        options.setMaxTransfers(Integer.MAX_VALUE);
        options.setMaxWalkDistance(Double.MAX_VALUE);

        // always use the threaded heuristic
        ThreadedBidirectionalHeuristic heuristic = 
                new ThreadedBidirectionalHeuristic(options.rctx.graph);

        options.rctx.remainingWeightHeuristic = heuristic;
        long searchBeginTime = System.currentTimeMillis();
        LOG.debug("BEGIN SEARCH");
        ShortestPathTree spt = sptService.getShortestPathTree(options, timeout);
        LOG.debug("END SUBSEARCH ({} msec)", System.currentTimeMillis() - searchBeginTime);
        heuristic.abort();
        
        if (spt == null) { // timeout or other fail
            LOG.warn("SPT was null.");
            return null;
        }
        paths = spt.getPaths();
        if (paths == null) {
            LOG.warn("Paths was null.");
            return null;
        }
        if (paths.size() == 0) {
            LOG.warn("Paths was 0-length.");
            return null;
        }
        // We order the list of returned paths by the time of arrival or departure (not path duration)
        Collections.sort(paths, new PathComparator(options.isArriveBy()));
        return paths;
    }

}
