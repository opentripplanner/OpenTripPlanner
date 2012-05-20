package org.opentripplanner.routing.automata;

public class Transition {

	public final int terminal;
	public final AutomatonState target;
	
	Transition (int token, AutomatonState target) {
		this.terminal = token;
		this.target = target;
		if (token < 0)
			throw new RuntimeException("negative token values are reserved");
	}
	
}
