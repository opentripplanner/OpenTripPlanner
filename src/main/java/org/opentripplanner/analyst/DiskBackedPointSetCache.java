package org.opentripplanner.analyst;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public class DiskBackedPointSetCache extends PointSetCache {
	
	private static final Logger LOG = LoggerFactory.getLogger(DiskBackedPointSetCache.class);

	protected File pointSetPath;
	
	protected class PointSetLoader extends CacheLoader<String, PointSet> {
	
		@Override
		public PointSet load(String pointSetId) throws Exception {
			if (pointSetPath != null && ! (pointSetPath.isDirectory() && pointSetPath.canRead())) {
                LOG.error("'{}' is not a readable directory.", pointSetPath);
                return null;
            }
    		for (File file : pointSetPath.listFiles()) {
                if(file.getName().toLowerCase().startsWith(pointSetId.toLowerCase())) {
					LOG.info("Attempting to load pointset from '{}'.", file);
                	PointSet pointSet = this.loadFromFile(file);
                	if(pointSet == null) {
						LOG.error("Pointset loading function returned null.");
						return null;
					} else {
                		return pointSet;
					}
                }
            }
			LOG.error("No file was found with the given pointset name.");
			return null;
		}
		
		public PointSet loadFromFile(File pointSetData) {
			
			String name = pointSetData.getName();
			
			if (name.endsWith(".csv")) {
	            String baseName = name.substring(0, name.length() - 4);
	            LOG.info("loading '{}' with ID '{}'", pointSetData, baseName);
	            try {
	                PointSet pset = PointSet.fromCsv(pointSetData);
	                if (pset == null) {
	                    LOG.warn("Failure, skipping this pointset.");
	                }
	                
	                return pset;
	                
	            } catch (IOException ioex) {
	                LOG.warn("Exception while loading pointset.", ioex);
	            }
	        } else if (name.endsWith(".json")) {
	            String baseName = name.substring(0, name.length() - 5);
	            LOG.info("loading '{}' with ID '{}'", pointSetData, baseName);
	            PointSet pset = PointSet.fromGeoJson(pointSetData);
	            if (pset == null) {
	                LOG.warn("Failure, skipping this pointset.");
	            }       	            
	            return pset;
	        }
			return null;
		}
	}

	public DiskBackedPointSetCache(Integer maxCacheSize, File pointSetPath) {
		super();
		
		this.pointSetPath = pointSetPath;
		
		// TODO could convert to a weight-based eviction strategy based on pointset size
		this.pointSets = CacheBuilder.newBuilder()
			       .maximumSize(maxCacheSize)
			       .build(new PointSetLoader());
		
	}

	@Override
	public ArrayList<String> getPointSetIds() {
		
		ArrayList<String> ids = new ArrayList<String>();
		
		for(File f : pointSetPath.listFiles()) {
			String name = f.getName();
			
			if (name.endsWith(".csv")) {
	            String baseName = name.substring(0, name.length() - 4);
	            ids.add(baseName);
			}
			else if (name.endsWith(".json")) {
		        String baseName = name.substring(0, name.length() - 5);
		        ids.add(baseName);
			}
		}

		return ids;
	}
	
}
