package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.automata.Nonterminal;

import static org.opentripplanner.routing.automata.Nonterminal.*;

import org.opentripplanner.routing.core.State;
import org.opentripplanner.routing.graph.Vertex;
import org.opentripplanner.routing.vertextype.IntersectionVertex;
import org.opentripplanner.routing.vertextype.TransitStop;


public class InitialStopSearchPathParser extends PathParser {
	
	private static final int STREET = 1;
	private static final int TRANSITSTOP = 2;
	private static final int OTHER = Integer.MAX_VALUE;

	private static DFA dfa;
	
	static {
		Nonterminal complete = seq(star(STREET), TRANSITSTOP);
		dfa = complete.toDFA().minimize();
	}
	
	@Override
	public int terminalFor(State state) {
		Vertex v = state.getVertex();
		
		if (v instanceof IntersectionVertex)
			return STREET;
		
		else if (v instanceof TransitStop)
			return TRANSITSTOP;
		
		else return OTHER;
	}

	@Override
	protected DFA getDFA() {
		return dfa;
	}

}
