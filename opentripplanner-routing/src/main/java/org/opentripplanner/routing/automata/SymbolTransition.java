package org.opentripplanner.routing.automata;

public class SymbolTransition extends Transition {

	SymbolTransition(int token, AutomatonState target) {
		super(token, target);
		// negative token values are reserved
		if (token < 0)
			throw new RuntimeException("negative token values are reserved");
	}

}
