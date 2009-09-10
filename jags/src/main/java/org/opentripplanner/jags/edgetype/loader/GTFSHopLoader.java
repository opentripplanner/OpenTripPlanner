package org.opentripplanner.jags.edgetype.loader;

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
	
	public void load(DrawHandler drawHandler) throws Exception {
		//Load stops
		feed.loadStops();
		for( Stop stop : feed.getAllStops() ) {
			graph.addVertex( stop.stop_id );
		}
		
		//Load hops
		GTFSHopFactory hf = new GTFSHopFactory(feed);
		for( Hop hop : hf.run() ) {
			if(drawHandler != null){ drawHandler.handle(hop); }
			graph.addEdge(hop.start.stop_id, hop.end.stop_id, hop);
		}
	}
	
	public void load() throws Exception {
		load(null);
	}
	
}
