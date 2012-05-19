package org.opentripplanner.routing.automata;

public class EpsilonTransition extends Transition {

	EpsilonTransition(AutomatonState target) {
		super(EPSILON, target);
	}

}
