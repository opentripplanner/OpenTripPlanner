package org.opentripplanner.routing.manytomany;

import java.io.File;

import org.opentripplanner.routing.core.GenericVertex;
import org.opentripplanner.routing.core.Graph;
import org.opentripplanner.routing.core.LowerBoundGraph;
import org.opentripplanner.routing.impl.GraphServiceImpl;

/**
 * An class that searches from every Individual in one Population
 * to every Individual in another Population, computing an aggregate over all
 * destination Individuals for each origin Individual.
 * 
 * @author andrewbyrd
 */
public class Searcher {

	Population origins;
	Population destinations;
	Graph graph;
	Aggregator aggregator;
	
	public Searcher (Graph graph, Population origins, Population destinations, Aggregator aggregator) {
		this.origins = origins;
		this.destinations = destinations;
		this.graph = graph;
		this.aggregator = aggregator;
	}
	
	public void search() {
		LowerBoundGraph lbg = new LowerBoundGraph(graph, LowerBoundGraph.OUTGOING);
		for (Individual origin : origins.elements) {
			System.out.println("Search from origin " + origin);
			destinations.clearResults();
			double[] results = lbg.sssp(origin.vertex);
			for (Individual destination : destinations.elements) {
				int idx = ((GenericVertex)destination.vertex).getIndex();
				destination.result = results[idx];
			}
			origin.result = aggregator.computeAggregate(destinations);
			System.out.println("    result was " + origin.result);
		}
	}
	
	public static void main (String[] args) {
		if (args.length != 4)
			throw new IllegalStateException("Must specify 4 arguments: path to graph, path to shapefile, name of the attribute, path to target geotiff");
		
//		Graph graph = loadGraph(args[0]);
		Population origins = Population.fromShapefile(args[1], args[2]);
		Population destinations = new RasterPopulation(args[3]);
		Aggregator aggregator = new ThresholdSumAggregator(30 * 60);
		//Aggregator aggregator = new ThresholdCountAggregator(15 * 60);
		
//		origins.link(graph);
//		destinations.link(graph);
//		Searcher searcher = new Searcher(graph, origins, destinations, aggregator);
//		searcher.search();
		origins.dump();
	}

	private static Graph loadGraph(String path) {
        GraphServiceImpl graphService = new GraphServiceImpl();
		File graphFile= new File(path);
        graphService.setGraphPath(graphFile);
        graphService.refreshGraph();
        return graphService.getGraph();
	}

}
