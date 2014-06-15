package org.opentripplanner.analyst;

import com.google.common.collect.Maps;

import org.opentripplanner.analyst.request.SampleFactory;
import org.opentripplanner.routing.services.GraphService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

public class PointSetCache {

    private static final Logger LOG = LoggerFactory.getLogger(PointSetCache.class);
    
    public final Map<String, PointSet> pointSets = Maps.newHashMap();
    
    private PointSetService pointSetService;
    
    public PointSetCache (PointSetService pointSetService) {
    	this.pointSetService = pointSetService;
    	
    }

    public PointSet get(String pointSetId) {

    	if(!pointSets.containsKey(pointSetId))
    		pointSets.put(pointSetId, pointSetService.getPointSet(pointSetId));
    	
    	return pointSets.get(pointSetId);
    }
}
