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

import org.onebusaway.gtfs.model.AgencyAndId;
import org.opentripplanner.common.model.NamedPlace;
import org.opentripplanner.routing.core.RoutingContext;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.error.TransitTimesException;
import org.opentripplanner.routing.error.VertexNotFoundException;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.pathparser.BasicPathParser;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.RemainingWeightHeuristicFactory;
import org.opentripplanner.routing.services.RoutingService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.opentripplanner.util.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

public class RetryingPathServiceImpl implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(RetryingPathServiceImpl.class);

    private static final int MAX_TIME_FACTOR = 2;
    private static final int MAX_WEIGHT_FACTOR = 2;

    @Autowired public GraphService graphService;
    @Autowired public SPTService sptService;

    private double firstPathTimeout = 0; // seconds
    private double multiPathTimeout = 0; // seconds
    
    /** Give up on searching for itineraries after this many seconds have elapsed. */
    public void setTimeout (double seconds) {
        firstPathTimeout = seconds;
        multiPathTimeout = seconds;
    }

    /**
     * Give up on searching for the first itinerary after this many seconds have elapsed.
     * A negative or zero value means search forever. 
     */
    public void setFirstPathTimeout (double seconds) {
        firstPathTimeout = seconds;
    }
    
    /**
     * Stop searching for additional itineraries (beyond the first one) after this many seconds 
     * have elapsed, relative to the beginning of the search for the first itinerary. 
     * A negative or zero value means search forever. 
     * Setting this lower than the firstPathTimeout will avoid searching for additional
     * itineraries when finding the first itinerary takes a long time. This helps keep overall 
     * response time down while assuring that the end user will get at least one response.
     */
    public void setMultiPathTimeout (double seconds) {
        multiPathTimeout = seconds;
    }

    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {

        ArrayList<GraphPath> paths = new ArrayList<GraphPath>();

        // make sure the options has a routing context *before* cloning it (otherwise you get
        // orphan RoutingContexts leaving temporary edges in the graph until GC)
        options.setRoutingContext(graphService.getGraph(options.getRouterId()));
        options.rctx.pathParsers = new PathParser[1];
        options.rctx.pathParsers[0] = new BasicPathParser();

        long searchBeginTime = System.currentTimeMillis();
        
        // The list of options specifying various modes, banned routes, etc to try for multiple
        // itineraries
        Queue<RoutingRequest> optionQueue = new LinkedList<RoutingRequest>();
        optionQueue.add(options);

        double maxWeight = Double.MAX_VALUE;
        double maxWalk = options.getMaxWalkDistance();
        long maxTime = options.isArriveBy() ? 0 : Long.MAX_VALUE;
        RoutingRequest currOptions;
        while (paths.size() < options.numItineraries) {
            currOptions = optionQueue.poll();
            if (currOptions == null) {
                LOG.debug("Ran out of options to try.");
                break;
            }
            currOptions.setMaxWalkDistance(maxWalk);
            
            // apply appropriate timeout
            double timeout = paths.isEmpty() ? firstPathTimeout : multiPathTimeout;
            
            // options.worstTime = maxTime;
            //options.maxWeight = maxWeight;
            long subsearchBeginTime = System.currentTimeMillis();
            
            LOG.debug("BEGIN SUBSEARCH");
            ShortestPathTree spt = sptService.getShortestPathTree(currOptions, timeout);
            if (spt == null) // timeout or other fail
                break;
            List<GraphPath> somePaths = spt.getPaths();
            LOG.debug("END SUBSEARCH ({} msec of {} msec total)", 
                    System.currentTimeMillis() - subsearchBeginTime,
                    System.currentTimeMillis() - searchBeginTime);
            if (somePaths == null) {
                // search failed, likely due to timeout
                // this could be signaled with an exception
                LOG.warn("Aborting search. {} paths found, elapsed time {} sec", 
                        paths.size(), (System.currentTimeMillis() - searchBeginTime) / 1000.0);
                break;
            }
            if (maxWeight == Double.MAX_VALUE) {
                /* the worst trip we are willing to accept is at most twice as bad or twice as long */
                if (somePaths.isEmpty()) {
                    // if there is no first path, there won't be any other paths
                    return null;
                }
                GraphPath path = somePaths.get(0);
                long duration = path.getDuration();
                LOG.debug("Setting max time and weight for subsequent searches.");
                LOG.debug("First path start time:  {}", path.getStartTime());
                maxTime = path.getStartTime() + 
                		  MAX_TIME_FACTOR * (currOptions.isArriveBy() ? -duration : duration);
                LOG.debug("First path duration:  {}", duration);
                LOG.debug("Max time set to:  {}", maxTime);
                maxWeight = path.getWeight() * MAX_WEIGHT_FACTOR;
                LOG.debug("Max weight set to:  {}", maxWeight);
                if (path.getWalkDistance() > maxWalk) {
                    maxWalk = path.getWalkDistance() * 1.25;
                }
            }
            if (somePaths.isEmpty()) {
                LOG.debug("No paths were found.");
                continue;
            }
            for (GraphPath path : somePaths) {
                if (!paths.contains(path)) {
                    if (path.getWalkDistance() > maxWalk) {
                        maxWalk = path.getWalkDistance() * 1.25;
                    }
                    paths.add(path);
                    // now, create a list of options, one with each trip in this journey banned.

                    LOG.debug("New trips: {}", path.getTrips());
                    RoutingRequest newOptions = currOptions.clone();
                    for (AgencyAndId trip : path.getTrips()) {
                        newOptions.bannedTrips.add(trip);
                    }
                    if (!optionQueue.contains(newOptions)) {
                        optionQueue.add(newOptions);
                    }
                }
            }
            LOG.debug("{} / {} itineraries", paths.size(), currOptions.numItineraries);
        }
        if (paths.size() == 0) {
            return null;
        }
        // We order the list of returned paths by the time of arrival or departure (not path duration)
        Collections.sort(paths, new PathComparator(options.isArriveBy()));
        return paths;
    }

}
