package org.opentripplanner.analyst;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.opentripplanner.routing.graph.Graph;
import org.opentripplanner.routing.services.GraphService;

public class LocalDiskPointSetService extends PointSetService {
	
	private static final Logger LOG = LoggerFactory.getLogger(LocalDiskPointSetService.class);
	
	File pointSetPath;

	public LocalDiskPointSetService(File pointSetPath, GraphService graphService) {
		super(graphService);
		this.pointSetPath = pointSetPath;
	}
	
	public PointSet load(File pointSetData) {
		
		String name = pointSetData.getName();
		
		if (name.endsWith(".csv")) {
            String baseName = name.substring(0, name.length() - 4);
            LOG.info("loading '{}' with ID '{}'", pointSetData, baseName);
            try {
                PointSet pset = PointSet.fromCsv(pointSetData.getAbsolutePath());
                if (pset == null) {
                    LOG.warn("Failure, skipping this pointset.");
                }
                
                pset.setGraphService(graphService);

                return pset;
                
            } catch (IOException ioex) {
                LOG.warn("Exception while loading pointset: {}", ioex);
            }
            
        } else if (name.endsWith(".json")) {
            String baseName = name.substring(0, name.length() - 5);
            LOG.info("loading '{}' with ID '{}'", pointSetData, baseName);
            PointSet pset = PointSet.fromGeoJson(pointSetData.getAbsolutePath());
            if (pset == null) {
                LOG.warn("Failure, skipping this pointset.");
            }
            
            pset.setGraphService(graphService);
            
            return pset;
        }
		
		return null;
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
	
	@Override
	public PointSet getPointSet(String pointSetId) {
	
		
		if (pointSetPath != null && ! (pointSetPath.isDirectory() && pointSetPath.canRead())) {
            LOG.error("'{}' is not a readable directory.", pointSetPath);
            return null;
        }
		
		for (File file : pointSetPath.listFiles()) {
           
            if(file.getName().toLowerCase().startsWith(pointSetId.toLowerCase())) {
            	
            	PointSet pointSet = load(file);
            	
            	if(pointSet != null)
            		return pointSet;
            	
            }
        }
		
		return null;
	}

}
