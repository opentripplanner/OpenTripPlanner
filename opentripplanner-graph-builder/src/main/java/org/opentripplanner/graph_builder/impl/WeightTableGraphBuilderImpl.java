package org.opentripplanner.graph_builder.impl;

import org.opentripplanner.graph_builder.services.GraphBuilder;
import org.opentripplanner.routing.algorithm.strategies.WeightTable;
import org.opentripplanner.routing.core.Graph;

/**
 * Add a weight table to a graph, which provides information to the
 * TableRemainingWeightHeuristic
 * This builder should be run after all transit and street data,
 * as well as transit-street links are in place.
 */
public class WeightTableGraphBuilderImpl implements GraphBuilder {
	private Double maxWalkSpeed = null;

	@Override
	public void buildGraph(Graph graph) {
		WeightTable wt = new WeightTable(graph);
		if (maxWalkSpeed != null) {
			wt.setMaxWalkSpeed(maxWalkSpeed);
		}
		wt.buildTable();
		graph.putService(WeightTable.class, wt);
	}

	/**
	 * The maximum walk speed that the weight table can support.  Using higher values
	 * than this during trip planning will lead to slower planning.
	 * @param maxWalkSpeed
	 */
	public void setMaxWalkSpeed(double maxWalkSpeed) {
		this.maxWalkSpeed = maxWalkSpeed;
	}

	public double getMaxWalkSpeed() {
		return maxWalkSpeed;
	}
}
