package org.opentripplanner.jags.edgetype.loader;

import java.util.ArrayList;

import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.edgetype.DrawHandler;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.jags.gtfs.Feed;
import org.opentripplanner.jags.gtfs.Stop;

public class GTFSHopLoader {
	Graph graph;
	Feed feed;
	
	public GTFSHopLoader( Graph graph, Feed feed ) {
		this.graph = graph;
		this.feed = feed;
	}
	
	public void load(DrawHandler drawHandler, boolean verbose) throws Exception {
		//Load stops
		if(verbose){ System.out.println( "Loading stops" ); }
		feed.loadStops();
		for( Stop stop : feed.getAllStops() ) {
			graph.addVertex( stop.stop_id );
		}
		
		//Load hops
		if(verbose){ System.out.println( "Loading hops" ); }
		GTFSHopFactory hf = new GTFSHopFactory(feed);
		int i=0;
		ArrayList<Hop> hops = hf.run(verbose);
		for( Hop hop : hops ) {
			i++;
			if(verbose && i%1000==0) { System.out.println( String.valueOf(i) ); }
			if(drawHandler != null){ drawHandler.handle(hop); }
			graph.addEdge(hop.start.stop_id, hop.end.stop_id, hop);
		}
	}
	
	public void load(DrawHandler drawHandler) throws Exception {
		load(drawHandler, false);
	}
	
	public void load() throws Exception {
		load(null);
	}
	
}
