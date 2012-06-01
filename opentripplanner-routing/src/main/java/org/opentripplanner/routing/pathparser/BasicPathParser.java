package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.edgetype.StreetEdge;
import org.opentripplanner.routing.graph.Edge;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextype.BikeRentalStationVertex;
import org.opentripplanner.routing.vertextype.OffboardVertex;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import static org.opentripplanner.routing.automata.Nonterminal.*;

/**
 * Some goals to build out for this path parser:
 * Reject paths that use crosswalks to 
 * Reject paths that take shortcuts through stops, by disallowing series of STL and requiring a transit trip after entering a station.  
 * Reject paths that reach the destination with a rented bicycle.
 * Disallow boarding transit without first parking a car.
 * Reject breaking no-through-traffic rules on driving legs.
 */
public class BasicPathParser extends PathParser {

	private static final int STATION = 1;
	private static final int TRANSIT = 2;
	private static final int STREET = 3; //these must match the class numbers in StreetEdge
	private static final int CROSSING = 4; //crosswalks
	private static final int OTHERPATH = 5;
	
	private static final DFA DFA;
	static {
	    
	        Nonterminal bikeNonStreet = star(choice(CROSSING, OTHERPATH));
	        
	        //(C|O)*(S+O(C|O)*)*(S*(C|O)*) -- the inverse of S+C+S+
	        Nonterminal optionalNontransitLeg = 
	                seq(bikeNonStreet, 
	                    star(seq(plus(STREET), OTHERPATH, bikeNonStreet)), 
	                    seq(star(STREET),bikeNonStreet));
	                
		Nonterminal transitLeg = seq(plus(STATION), plus(TRANSIT), plus(STATION));
		Nonterminal itinerary = seq(optionalNontransitLeg, star(transitLeg, optionalNontransitLeg));
		DFA = itinerary.toDFA().minimize();
		System.out.println(DFA.toGraphViz());
		System.out.println(DFA.dumpTable());
	}
	
	@Override
	protected DFA getDFA() {
		return DFA;
	}
	
	@Override
	public int terminalFor(State state) {
		Vertex v = state.getVertex();
		if (v instanceof StreetVertex || v instanceof StreetLocation) {
		        Edge edge = state.getBackEdge();
			if (edge instanceof StreetEdge) {
			    int cls = ((StreetEdge) edge).getStreetClass();
			    return cls;
			} else {
			    return OTHERPATH;
			}
		}
		if (v instanceof OnboardVertex)
			return TRANSIT;
		if (v instanceof OffboardVertex)
			return STATION;
		if (v instanceof BikeRentalStationVertex)
                    return OTHERPATH;
		else 
			throw new RuntimeException("failed to tokenize path");
	}
	
}
