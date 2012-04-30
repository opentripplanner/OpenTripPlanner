package org.opentripplanner.analyst.request;

import org.opentripplanner.routing.core.TraverseOptions;
import org.opentripplanner.routing.services.GraphService;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.routing.spt.ShortestPathTree;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;

@Component
public class SPTCache extends CacheLoader<TraverseOptions, ShortestPathTree> {

    private static final Logger LOG = LoggerFactory.getLogger(SPTCache.class);

    @Autowired private SPTService sptService; 
    
    @Autowired private GraphService graphService; 

    private LoadingCache<TraverseOptions, ShortestPathTree> sptCache = CacheBuilder
            .newBuilder()
            .concurrencyLevel(16)
            .maximumSize(16)
            .build(this);

    @Override /** completes the abstract CacheLoader superclass */
    public ShortestPathTree load(TraverseOptions req) throws Exception {
        LOG.debug("spt cache miss : {}", req);
        req.setRoutingContext(graphService.getGraph());
        long t0 = System.currentTimeMillis();
        ShortestPathTree spt = sptService.getShortestPathTree(req);
        long t1 = System.currentTimeMillis();
        LOG.debug("calculated spt in {}msec", (int) (t1 - t0));
        return spt;
    }

    public ShortestPathTree get(TraverseOptions req) throws Exception {
        return req == null ? null : sptCache.get(req);
    }
    
}
