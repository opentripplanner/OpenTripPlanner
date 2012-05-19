package org.opentripplanner.routing.automata;

public abstract class Transition {

	public static final int EPSILON = Integer.MIN_VALUE;
	
	public final int token;
	public final AutomatonState target;
	
	Transition (int token, AutomatonState target) {
		this.token = token;
		this.target = target;
	}
	
}
