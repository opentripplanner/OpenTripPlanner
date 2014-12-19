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
import java.util.Collections;
import java.util.List;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.pathparser.BasicPathParser;
import org.opentripplanner.routing.pathparser.NoThruTrafficPathParser;
import org.opentripplanner.routing.pathparser.PathParser;
import org.opentripplanner.routing.services.PathService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.GraphPath;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ParetoPathService implements PathService {

    private static final Logger LOG = LoggerFactory.getLogger(ParetoPathService.class);

    private Graph graph;
    private SPTServiceFactory sptServiceFactory;
    
    private SPTVisitor sptVisitor = null;

    private double timeout = 0; // seconds
    
    public ParetoPathService(Graph graph, SPTServiceFactory spts) {
        this.graph = graph;
        this.sptServiceFactory = spts;
    }

	/** Give up on searching for itineraries after this many seconds have elapsed. */
    public void setTimeout (double seconds) {
        timeout = seconds;
    }

    public void setSPTVisitor(SPTVisitor sptVisitor) {
        this.sptVisitor = sptVisitor;
    }

    @Override
    public List<GraphPath> getPaths(RoutingRequest options) {
    	
    	SPTService sptService = this.sptServiceFactory.instantiate();

        ArrayList<GraphPath> paths = new ArrayList<GraphPath>();

        // make sure the options has a routing context *before* cloning it (otherwise you get
        // orphan RoutingContexts leaving temporary edges in the graph until GC)
        if (options.rctx == null) {
            options.setRoutingContext(graph);
            options.rctx.pathParsers = new PathParser[] { new BasicPathParser(),
                    new NoThruTrafficPathParser() };
        }

        long searchBeginTime = System.currentTimeMillis();
        
        ShortestPathTree spt = sptService.getShortestPathTree(options, timeout);
        
        if(sptVisitor!=null){
        	System.out.println( "setting spt" );
        	sptVisitor.spt = spt;
        } else {
        	System.out.println( "no spt visitor" );
        }
        
        if (spt == null) {
            // Serious failure, no paths provided. This could be signaled with an exception.
            LOG.warn("Aborting search. {} paths found, elapsed time {} sec", 
                    paths.size(), (System.currentTimeMillis() - searchBeginTime) / 1000.0);
            return null;
        }
        for( GraphPath gp : spt.getPaths() ) {
        	paths.add(gp);
        }
        LOG.debug("SPT provides {} paths to target.", paths.size());

        LOG.debug("{} / {} itineraries", paths.size(), options.numItineraries);
        if (options.rctx.aborted) {
            // search was cleanly aborted, probably due to a timeout. 
            // There may be useful paths, but we should stop retrying.
            return null;
        }

        if (paths.size() == 0) {
            return null;
        }
        
        // We order the list of returned paths by the time of arrival or departure (not path duration)
        Collections.sort(paths, new PathComparator(options.arriveBy));
        return paths;
    }

    public SPTServiceFactory getSptServiceFactory() {
        return sptServiceFactory;
    }

    public void setSptServiceFactory(SPTServiceFactory sptServiceFactory) {
        this.sptServiceFactory = sptServiceFactory;
    }

}