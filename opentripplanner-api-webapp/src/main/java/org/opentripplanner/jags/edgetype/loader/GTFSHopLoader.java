package org.opentripplanner.jags.edgetype.loader;

import org.onebusaway.gtfs.model.AgencyAndId;
import org.onebusaway.gtfs.model.Stop;
import org.onebusaway.gtfs.services.GtfsRelationalDao;

import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.SpatialVertex;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.edgetype.DrawHandler;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.factory.GTFSHopFactory;
import org.opentripplanner.jags.gtfs.GtfsContext;

import java.util.ArrayList;

public class GTFSHopLoader {
	
  private Graph _graph;
  
  private GtfsRelationalDao _dao;

  private GtfsContext _context;
	
	public GTFSHopLoader( Graph graph, GtfsContext context) {
		_graph = graph;
		_context = context;
		_dao = context.getDao();
	}
	
	public void load(DrawHandler drawHandler, boolean verbose) throws Exception {

	  //Load stops
		for( Stop stop : _dao.getAllStops() ) {
			_graph.addVertex( new SpatialVertex(id(stop.getId()), stop.getLat(), stop.getLon()) );
		}
		
		//Load hops
		if(verbose){ System.out.println( "Loading hops" ); }
		GTFSHopFactory hf = new GTFSHopFactory(_context);
		ArrayList<Hop> hops = hf.run(verbose);
		for( Hop hop : hops ) {
			if(drawHandler != null){ drawHandler.handle(hop); }
			Vertex start = _graph.addVertex(id(hop.getStartStopTime().getStop().getId()));
			start.isTransitStop = true;
			Vertex end = _graph.addVertex(id(hop.getEndStopTime().getStop().getId()));
			end.isTransitStop = true;
			_graph.addEdge(start, end, hop);
		}
	}
	
	public void load(DrawHandler drawHandler) throws Exception {
		load(drawHandler, false);
	}
	
	public void load() throws Exception {
		load(null);
	}
	
	private String id(AgencyAndId id) {
	  return id.getAgencyId() + "_" + id.getId();
	}
	
}
