package org.opentripplanner.analyst;

import com.google.common.cache.LoadingCache;
import com.google.common.collect.Maps;

import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Map;

public abstract class PointSetCache {

    private static final Logger LOG = LoggerFactory.getLogger(PointSetCache.class);
    
    LoadingCache<String, PointSet> pointSets;
    
    protected GraphService graphService;
    
    public PointSetCache (GraphService graphService) {
    	this.graphService = graphService;
    }

    public  PointSet get(String pointSetId) {
    	return pointSets.getUnchecked(pointSetId);
    };
	
	public abstract List<String> getPointSetIds();
}
