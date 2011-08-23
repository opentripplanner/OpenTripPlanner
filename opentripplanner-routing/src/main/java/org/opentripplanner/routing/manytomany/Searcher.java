package org.opentripplanner.routing.manytomany;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

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
		PrintStream ps = null;
		try {
			ps = new PrintStream(new File("/home/syncopate/searcher.csv"));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		for (Individual origin : origins.elements) {
			if (origin.vertex == null) {
				origin.result = Double.NaN;
				continue;
			}
			System.out.println("Search from origin " + origin);
			//destinations.clearResults();
			double[] results = lbg.sssp(origin.vertex);
			for (Individual destination : destinations.elements) {
				if (destination.vertex == null) {
					destination.result = Double.NaN;
					continue;
				}
				int idx = ((GenericVertex)destination.vertex).getIndex();
				destination.result = results[idx];
			}
			origin.result = aggregator.computeAggregate(destinations);
			System.out.println("    result was " + origin.result);
			ps.printf("%f;%f;%f;%f\n", origin.x, origin.y, origin.data, origin.result);
		}
	}
	
	public static void main (String[] args) {
		if (args.length != 4)
			throw new IllegalStateException("Must specify 4 arguments: path to graph, path to shapefile, name of the attribute, path to target geotiff");
		
		Graph graph = loadGraph(args[0]);
		Population origins = Population.fromShapefile(args[1], args[2]);
		Population destinations = new RasterPopulation(args[3]);
		//Aggregator aggregator = new ThresholdSumAggregator(30 * 60);
		Aggregator aggregator = new ThresholdCumulativeAggregator(90 * 60);
		
		origins.link(graph);
		destinations.link(graph);
		Searcher searcher = new Searcher(graph, origins, destinations, aggregator);
		searcher.search();
		origins.dump();
		destinations.dump();
	}

	private static Graph loadGraph(String path) {
        GraphServiceImpl graphService = new GraphServiceImpl();
		File graphFile= new File(path);
        graphService.setGraphPath(graphFile);
        graphService.refreshGraph();
        return graphService.getGraph();
	}

}
