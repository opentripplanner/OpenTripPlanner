package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.algorithm.GenericAStar;
import org.opentripplanner.routing.services.SPTService;
import org.opentripplanner.visualizer.VisualTraverseVisitor;

public class GenericAStarFactory implements SPTServiceFactory{

	private VisualTraverseVisitor traverseVisitor=null;

	@Override
	public SPTService instantiate() {
		GenericAStar ret = new GenericAStar();
		if(traverseVisitor!=null){
			ret.setTraverseVisitor(traverseVisitor);
		}
		return ret;
	}

	public void setTraverseVisitor(VisualTraverseVisitor visitor) {
		this.traverseVisitor = visitor;
	}

}
