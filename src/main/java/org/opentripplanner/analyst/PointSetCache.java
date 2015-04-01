package org.opentripplanner.analyst;

import com.google.common.cache.LoadingCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * This is a loadingCache, so it will attempt to load pointsets only when they are requested.
 * Therefore loading errors will only surface when pointsets are first used.
 */
public abstract class PointSetCache {

    private static final Logger LOG = LoggerFactory.getLogger(PointSetCache.class);
    
    protected LoadingCache<String, PointSet> pointSets;
        
    public PointSetCache () {
    }

    public  PointSet get(String pointSetId) {
    	return pointSets.getUnchecked(pointSetId);
    };
	
	public abstract List<String> getPointSetIds();
}
