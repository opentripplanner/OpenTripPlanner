package org.opentripplanner.routing.automata;

import java.util.ArrayList;

public class AutomatonState extends ArrayList<Transition> {

	private static final long serialVersionUID = 42L;

	/** Signals that no transition was found for a given input symbol. The input is rejected. */
	public static final int REJECT = Integer.MIN_VALUE;

	/** Could be used to provide a single accept state, using transitions on a special terminal from all other accept states. */
	public static final int ACCEPT = Integer.MAX_VALUE;

	/** Indicates whether input is to be accepted if parsing ends at this AutomatonState instance. */
	public boolean accept = false;
	
}
