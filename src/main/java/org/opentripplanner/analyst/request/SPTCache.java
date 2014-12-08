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

package org.opentripplanner.analyst.request;

import org.opentripplanner.routing.core.RoutingRequest;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.impl.SPTServiceFactory;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

public class SPTCache extends CacheLoader<RoutingRequest, ShortestPathTree> {

    private static final Logger LOG = LoggerFactory.getLogger(SPTCache.class);

    private SPTServiceFactory sptServiceFactory;
    
    private Graph graph;

    public SPTCache(SPTServiceFactory sptServiceFactory, Graph graph) {
        this.sptServiceFactory = sptServiceFactory;
        this.graph = graph;
        this.sptCache = CacheBuilder.newBuilder()
                .concurrencyLevel(concurrency)
                .maximumSize(size)
                .build(this);
    }

    private LoadingCache<RoutingRequest, ShortestPathTree> sptCache;

    public int size = 200;
    public int concurrency = 16;

    @Override /** completes the abstract CacheLoader superclass */
    public ShortestPathTree load(RoutingRequest req) throws Exception {
        LOG.debug("spt cache miss : {}", req);
        req.setRoutingContext(graph);
        long t0 = System.currentTimeMillis();
        ShortestPathTree spt = sptServiceFactory.instantiate().getShortestPathTree(req);
        long t1 = System.currentTimeMillis();
        LOG.debug("calculated spt in {}msec", (int) (t1 - t0));
        req.cleanup();
        return spt;
    }

    public ShortestPathTree get(RoutingRequest req) throws Exception {
        return req == null ? null : sptCache.get(req);
    }
    
}
