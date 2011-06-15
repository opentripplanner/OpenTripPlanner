package org.opentripplanner.routing.impl;

import org.opentripplanner.routing.core.AbstractEdge;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.core.StateEditor;
import org.opentripplanner.routing.core.TraverseMode;
import org.opentripplanner.routing.core.Vertex;

import com.vividsolutions.jts.geom.Geometry;

/* This edge, because it has no mode, initiates another leg.
 */
public class LegSwitchingEdge extends AbstractEdge {

	private static final long serialVersionUID = 1L;

	public LegSwitchingEdge(Vertex v1, Vertex v2) {
		super(v1, v2);
	}

	@Override
	public State traverse(State s0) {
		StateEditor editor = s0.edit(this);
		return editor.makeState();
	}

	@Override
	public double getDistance() {
		return 0;
	}

	@Override
	public Geometry getGeometry() {
		return null;
	}

	@Override
	public TraverseMode getMode() {
		return null;
	}

	@Override
	public String getName() {
		return null;
	}

}
