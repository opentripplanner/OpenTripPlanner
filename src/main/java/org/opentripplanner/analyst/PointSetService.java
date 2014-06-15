package org.opentripplanner.analyst;

import java.util.List;

import org.opentripplanner.routing.services.GraphService;

public abstract class PointSetService {
	
	protected GraphService graphService;
	
	public PointSetService(GraphService graphService) {
		this.graphService = graphService;
	}

	public abstract PointSet getPointSet(String pointSetId);
	
	public abstract List<String> getPointSetIds();

}
