package org.opentripplanner.jags.algorithm.kao;

import org.opentripplanner.jags.core.Edge;
import org.opentripplanner.jags.core.Graph;
import org.opentripplanner.jags.core.State;
import org.opentripplanner.jags.core.Vertex;
import org.opentripplanner.jags.core.WalkOptions;
import org.opentripplanner.jags.core.WalkResult;
import org.opentripplanner.jags.edgetype.Hop;
import org.opentripplanner.jags.edgetype.Walkable;
import org.opentripplanner.jags.gtfs.GtfsContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;


public class KaoGraph extends Graph {
	private static final long serialVersionUID = 3667189924531545548L;
	
	ArrayList<Edge> allhops;

  private GtfsContext _context;
	
  public KaoGraph() {
		allhops = new ArrayList<Edge>();
	}
	
	public void setGtfsContext(GtfsContext context) {
	  _context = context;
  }
	
	public Edge addEdge( Vertex a, Vertex b, Walkable ep ) {
    	Edge ret = super.addEdge(a, b, ep);
    	allhops.add(ret);
    	return ret;
    }
	
	public ArrayList<EdgeOption> sortedEdges(GregorianCalendar time, long window) {
		ArrayList<EdgeOption> ret = new ArrayList<EdgeOption>();
		State state0 = new State(time);
		
		for(int i=0; i<allhops.size(); i++) {
			Edge hopedge = allhops.get(i);
			Hop hop = (Hop)hopedge.payload;
			
			WalkOptions options = new WalkOptions();
			options.setGtfsContext(_context);
			
			WalkResult wr = hop.walk(state0, options);
			
			if( wr != null ) {
				long timeToArrival = wr.state.time.getTimeInMillis() - time.getTimeInMillis();
				if( timeToArrival <= window ) {
					ret.add( new EdgeOption(hopedge, timeToArrival) );
				}
			}
		}
		
		Collections.sort(ret);
		
		return ret;
	}

  
}

