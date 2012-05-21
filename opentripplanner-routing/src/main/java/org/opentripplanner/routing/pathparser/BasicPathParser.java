package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;
import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.location.StreetLocation;
import org.opentripplanner.routing.vertextype.OffboardVertex;
import org.opentripplanner.routing.vertextype.OnboardVertex;
import org.opentripplanner.routing.vertextype.StreetVertex;

import static org.opentripplanner.routing.automata.Nonterminal.*;

/**
 * Some goals to build out for this path parser:
 * Reject paths that take shortcuts through stops, by disallowing series of STL and requiring a transit trip after entering a station.  
 * Reject paths that reach the destination with a rented bicycle.
 * Disallow boarding transit without first parking a car.
 * Reject breaking no-through-traffic rules on driving legs.
 */
public class BasicPathParser extends PathParser {

	private static final int WALK = 1;
	private static final int STATION = 2;
	private static final int TRANSIT = 3;
	
	private static final DFA DFA;
	static {
		Nonterminal optionalWalkLeg = star(WALK);
		Nonterminal transitLeg = seq(plus(STATION), plus(TRANSIT), plus(STATION));
		Nonterminal itinerary = seq(optionalWalkLeg, star(transitLeg, optionalWalkLeg));
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
		if (v instanceof StreetVertex || v instanceof StreetLocation)
			return WALK;
		if (v instanceof OnboardVertex)
			return TRANSIT;
		if (v instanceof OffboardVertex)
			return STATION;
		else 
			throw new RuntimeException("failed to tokenize path");
	}
	
}
