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
	@Override
	public void buildGraph(Graph graph) {
		WeightTable wt = new WeightTable(graph);
		graph.putService(WeightTable.class, wt);
	}
}
