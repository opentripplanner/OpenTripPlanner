package org.opentripplanner.routing.automata;

import java.util.ArrayList;
import java.util.List;

public class AutomatonState {

	/** Signals that no transition was found for a given input symbol. The input is rejected. */
	public static final int REJECT = Integer.MIN_VALUE;
	
	/** Could be used to provide a single accept state, using transitions on a special terminal from all other accept states. */
	public static final int ACCEPT = Integer.MAX_VALUE;

	/** The states in a DFA should be ordered such that the start state is always 0. */
	public static final int START = 0;

	private static char nextLabel = 'A';
	
	public String label;
	
	public final List<Transition> transitions = new ArrayList<Transition>(); 
	
	public final List<AutomatonState> epsilonTransitions = new ArrayList<AutomatonState>(); 
	
	public AutomatonState() {
		this(Character.toString(nextLabel));
		nextLabel++;
		if (nextLabel > 'Z')
			nextLabel = 'A';
	}
	
	public AutomatonState(String label) {
		this.label = label;
		//System.out.println(this.toString());
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("AutomatonState ");
		sb.append(label);
		sb.append(" transitions ");
		for (Transition transition : this.transitions) {
			sb.append(transition.terminal);
			sb.append("-");
			sb.append(transition.target.label);
			sb.append(" ");
		}
		sb.append(" epsilon moves {");
		for (AutomatonState as : this.epsilonTransitions) {
			sb.append(as.label);
			sb.append(" ");
		}
		sb.append("}");
		return sb.toString();
	}
	
}
