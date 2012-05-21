package org.opentripplanner.routing.pathparser;

import org.opentripplanner.routing.automata.DFA;
import org.opentripplanner.routing.core.State;

public abstract class PathParser {

	public int transition(int initState, int terminal) {
		return this.getDFA().transition(initState, terminal);
	}
	
	public boolean accepts(int parseState) {
		return this.getDFA().accepts(parseState);
	}

	/** 
	 * Concrete PathParsers implement this method to convert OTP States 
	 * (and their backEdges) into terminals in the language they define.
	 */
	public abstract int terminalFor(State state);
	
	/** 
	 * Concrete PathParsers implement this method to provide a DFA that
	 * will accept certain paths and not others. 
	 */
	protected abstract DFA getDFA();

}